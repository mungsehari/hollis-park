package com.hollispark.store.service.user;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Locks an email for 15 minutes after 5 failed sign-ins. Per API instance; add WAF rate limits in production. */
@Component
public class LoginAttempts {
    private static final int MAX = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Entry(int failures, Instant firstAt, Instant lockedUntil) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public boolean locked(String email) {
        Entry e = entries.get(key(email));
        return e != null && e.lockedUntil() != null && Instant.now().isBefore(e.lockedUntil());
    }

    public void failed(String email) {
        Instant now = Instant.now();
        entries.compute(key(email), (k, e) -> {
            if (e == null || now.isAfter(e.firstAt().plus(WINDOW))) return new Entry(1, now, null);
            int n = e.failures() + 1;
            return new Entry(n, e.firstAt(), n >= MAX ? now.plus(WINDOW) : null);
        });
    }

    public void succeeded(String email) {
        entries.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
