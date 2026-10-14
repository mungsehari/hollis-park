package com.hollispark.store.controller.marketing;

import com.hollispark.store.enums.Marketplace;
import com.hollispark.store.service.marketing.OutboundService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The store's "Buy on Amazon / Flipkart" links point here. The click is counted, then the browser is sent on.
 * Only stored, validated links are ever used, so this cannot be abused as an open redirect.
 * When a link no longer exists the visitor lands back on the store instead of an error page.
 */
@RestController
@RequestMapping("/api/go")
@RequiredArgsConstructor
public class OutboundController {
    private final OutboundService outbound;

    @GetMapping("/product/{slug}/{marketplace}")
    public ResponseEntity<Void> product(@PathVariable String slug, @PathVariable String marketplace) {
        Marketplace m = Marketplace.parse(marketplace);
        String target = m == null ? null : outbound.productLink(slug, m).orElse(null);
        return redirect(target, "/catalog/" + slug);
    }

    @GetMapping("/pick/{id}")
    public ResponseEntity<Void> pick(@PathVariable UUID id) {
        return redirect(outbound.pickLink(id).orElse(null), "/care");
    }

    private static ResponseEntity<Void> redirect(String target, String fallback) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target != null ? target : fallback))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Robots-Tag", "noindex, nofollow")
                .build();
    }
}
