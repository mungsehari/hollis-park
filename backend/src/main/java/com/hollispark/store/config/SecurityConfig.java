package com.hollispark.store.config;

import com.hollispark.store.domain.StaffUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * The storefront API is public. Everything under /api/admin needs a staff session:
 * STAFF can work orders and messages, ADMIN can also edit products, settings, promo codes and staff.
 * Sessions are stored in PostgreSQL (Spring Session JDBC), so several API instances can share them.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookieCustomizer(c -> c.path("/").sameSite("Lax"));

        http
            .csrf(c -> c
                .csrfTokenRepository(csrfRepo)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Anonymous storefront forms: no session involved, so no CSRF token needed.
                .ignoringRequestMatchers("/api/orders", "/api/orders/*/payment", "/api/contact", "/api/newsletter",
                        "/api/promo/validate", "/api/payments/razorpay/webhook"))
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/admin/staff/**", "/api/admin/promo-codes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/admin/products/**", "/api/admin/settings/**", "/api/admin/collections/**",
                        "/api/admin/home/**", "/api/admin/media/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/auth/me", "/api/auth/password", "/api/auth/logout").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> json(res, 401, "Please sign in."))
                .accessDeniedHandler((req, res, ex) -> json(res, 403, "You do not have access to this.")))
            .securityContext(s -> s.securityContextRepository(securityContextRepository()))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable())
            .headers(h -> h
                .frameOptions(f -> f.deny())
                // The API only returns JSON and images: nothing it sends may load scripts or be framed.
                .contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                .httpStrictTransportSecurity(s -> s.includeSubDomains(true).maxAgeInSeconds(31536000))
                .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()"))
                .referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        return http.build();
    }

    private static void json(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"error\":\"" + (status == 401 ? "Unauthorized" : "Forbidden") + "\",\"message\":\"" + message + "\"}");
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(StaffUserRepository staff) {
        return email -> staff.findByEmailIgnoreCase(email)
                .map(u -> User.withUsername(u.getEmail()).password(u.getPasswordHash()).roles(u.getRole().name())
                        .disabled(!u.isActive()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
