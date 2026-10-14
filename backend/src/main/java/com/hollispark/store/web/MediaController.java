package com.hollispark.store.web;

import com.hollispark.store.media.MediaStorage;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves /media/** from the storage backend when it is S3 (local storage is served by a resource
 * handler instead). Keys are random, so responses are cached for a year.
 */
@RestController
public class MediaController {
    private final MediaStorage storage;

    public MediaController(MediaStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/media/**")
    public ResponseEntity<byte[]> media(HttpServletRequest request) {
        String key = request.getRequestURI().substring("/media/".length());
        if (key.isBlank() || key.contains("..") || key.startsWith("/")) return ResponseEntity.notFound().build();
        MediaStorage.Stored s = storage.get(key);
        if (s == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .contentType(MediaType.parseMediaType(s.contentType() == null ? "application/octet-stream" : s.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(s.bytes());
    }
}
