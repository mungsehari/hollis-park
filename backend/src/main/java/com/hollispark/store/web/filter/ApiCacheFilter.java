package com.hollispark.store.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * HTTP caching rules for the API. Public catalog reads carry an ETag and must be revalidated, so a browser
 * that already has the current data gets a tiny 304 and an admin edit shows up on the next load.
 * Everything else under /api (orders, staff data, sign-in) is never stored by browsers or proxies.
 */
@Configuration
public class ApiCacheFilter {
    private static final String[] CATALOG_READS = {"/api/store", "/api/products", "/api/products/*", "/api/collections", "/api/reviews"};

    static class CacheControlFilter extends OncePerRequestFilter {
        @Override
        protected boolean shouldNotFilter(HttpServletRequest req) {
            return !req.getRequestURI().startsWith("/api/");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String uri = req.getRequestURI();
            boolean readOnly = "GET".equals(req.getMethod()) || "HEAD".equals(req.getMethod());
            boolean catalogRead = readOnly && !uri.startsWith("/api/admin") && !uri.startsWith("/api/auth")
                    && !uri.startsWith("/api/go/");
            res.setHeader(HttpHeaders.CACHE_CONTROL, catalogRead ? "public, no-cache" : "no-store");
            chain.doFilter(req, res);
        }
    }

    @Bean
    FilterRegistrationBean<CacheControlFilter> apiCacheControl() {
        var reg = new FilterRegistrationBean<>(new CacheControlFilter());
        reg.addUrlPatterns("/api/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return reg;
    }

    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> catalogEtag() {
        // Weak ETags: Tomcat refuses to gzip a response that carries a strong one.
        var etag = new ShallowEtagHeaderFilter();
        etag.setWriteWeakETag(true);
        var reg = new FilterRegistrationBean<>(etag);
        reg.addUrlPatterns(CATALOG_READS);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 30);
        return reg;
    }
}
