package com.hollispark.store.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hollispark.store.config.StoreProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client-IP limits on the public forms and on sign-in, so one address cannot flood orders, the inbox or
 * password guesses. Counts live in memory per API instance (fixed windows); put AWS WAF rate rules in front for
 * a shared, network-wide limit. The client address is the real one: Tomcat reads X-Forwarded-For from the
 * trusted proxies (see server.forward-headers-strategy).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {
    private record Rule(String path, int limit, Duration window, Cache<String, AtomicInteger> hits) {
        static Rule of(String path, int limit, Duration window) {
            return new Rule(path, limit, window,
                    Caffeine.newBuilder().expireAfterWrite(window).maximumSize(100_000).build());
        }
    }

    private final boolean enabled;
    private final List<Rule> rules;

    public RateLimitFilter(StoreProperties props) {
        this.enabled = props.rateLimit() == null || props.rateLimit().enabled();
        int scale = props.rateLimit() == null || props.rateLimit().perTenMinutes() <= 0 ? 30 : props.rateLimit().perTenMinutes();
        Duration ten = Duration.ofMinutes(10);
        this.rules = List.of(
                Rule.of("/api/orders", scale, ten),
                Rule.of("/api/promo/validate", scale, ten),
                Rule.of("/api/contact", Math.max(3, scale / 3), ten),
                Rule.of("/api/newsletter", Math.max(3, scale / 3), ten),
                Rule.of("/api/auth/login", Math.max(5, scale / 2), Duration.ofMinutes(15)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !enabled || !"POST".equals(req.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        for (Rule rule : rules) {
            if (!path.equals(rule.path())) continue;
            int n = rule.hits().get(req.getRemoteAddr(), k -> new AtomicInteger()).incrementAndGet();
            if (n > rule.limit()) {
                res.setStatus(429);
                res.setHeader("Retry-After", String.valueOf(rule.window().toSeconds()));
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Too many requests. Please wait a few minutes and try again.\"}");
                return;
            }
            break;
        }
        chain.doFilter(req, res);
    }
}
