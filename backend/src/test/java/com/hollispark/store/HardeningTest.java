package com.hollispark.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hollispark.store.entity.marketing.PromoCode;
import com.hollispark.store.repository.marketing.PromoCodeRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Audit columns, optimistic locking, request ids, HTTP caching, security headers and rate limits. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "store.media.local-dir=target/test-media", "store.razorpay.key-id=", "store.razorpay.key-secret=",
        "store.rate-limit.enabled=true", "store.rate-limit.per-ten-minutes=3"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES, provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class HardeningTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    PromoCodeRepository promos;

    @Test
    void baseEntityFillsIdAuditColumnsAndRefusesStaleWrites() {
        PromoCode p = promos.saveAndFlush(PromoCode.builder().code("AUDIT1").rate(new BigDecimal("0.0500")).build());
        try {
            assertThat(p.getId()).isNotNull();
            assertThat(p.getCreatedAt()).isNotNull();
            assertThat(p.getUpdatedAt()).isNotNull();
            assertThat(p.getVersion()).isZero();

            PromoCode stale = promos.findById(p.getId()).orElseThrow();
            PromoCode fresh = promos.findById(p.getId()).orElseThrow();
            fresh.setRate(new BigDecimal("0.0700"));
            fresh = promos.saveAndFlush(fresh);
            assertThat(fresh.getVersion()).isEqualTo(1L);
            assertThat(fresh.getUpdatedAt()).isAfterOrEqualTo(fresh.getCreatedAt());
            assertThat(fresh.getCreatedAt()).isEqualTo(p.getCreatedAt());

            stale.setRate(new BigDecimal("0.0900"));
            assertThatThrownBy(() -> promos.saveAndFlush(stale)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        } finally {
            promos.deleteById(p.getId());
        }
    }

    @Test
    void requestIdIsEchoedOrCreated() throws Exception {
        mvc.perform(get("/api/v1/products").header("X-Request-Id", "trace-12345678"))
                .andExpect(header().string("X-Request-Id", "trace-12345678"));
        String generated = mvc.perform(get("/api/v1/products").header("X-Request-Id", "bad id!"))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(generated).hasSize(36);
    }

    @Test
    void errorsCarryTheRequestId() throws Exception {
        mvc.perform(get("/api/v1/products/nope").header("X-Request-Id", "trace-87654321"))
                .andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.requestId").value("trace-87654321"));
    }

    @Test
    void catalogReadsAreRevalidatedWithEtag() throws Exception {
        var first = mvc.perform(get("/api/v1/store"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, no-cache"))
                .andExpect(header().exists("ETag"))
                .andReturn().getResponse();
        mvc.perform(get("/api/v1/store").header("If-None-Match", first.getHeader("ETag")))
                .andExpect(status().isNotModified());
    }

    @Test
    void nonCatalogApiIsNeverCached() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void apiSendsSecurityHeaders() throws Exception {
        mvc.perform(get("/api/v1/products"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void repeatedFormPostsAreRateLimited() throws Exception {
        String body = "{\"code\":\"NOPE\"}";
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/v1/promo/validate").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/promo/validate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
