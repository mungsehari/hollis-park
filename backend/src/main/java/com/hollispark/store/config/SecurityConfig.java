package com.hollispark.store.config;

import com.hollispark.store.repository.user.StaffUserRepository;
import com.hollispark.store.util.ApiErrorWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
 * The storefront API is public. Everything under /api/v1/admin needs a staff session. Which role may do
 * what is declared twice on purpose: on each controller with {@code @PreAuthorize} (see {@link Access}),
 * and as URL rules here. The URL rules run before the request body is parsed and validated, and they
 * keep an endpoint protected even if its annotation is forgotten. Change both together.
 * Sessions are stored in PostgreSQL (Spring Session JDBC), so several API instances can share them.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ApiErrorWriter errors) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookieCustomizer(c -> c.path("/").sameSite("Lax"));

        http
            .csrf(c -> c
                .csrfTokenRepository(csrfRepo)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Anonymous storefront forms: no session involved, so no CSRF token needed.
                .ignoringRequestMatchers("/api/v1/orders", "/api/v1/orders/*/payment", "/api/v1/contact", "/api/v1/newsletter",
                        "/api/v1/promo/validate", "/api/v1/payments/razorpay/webhook"))
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(a -> a
                // Same rules as the controllers' @PreAuthorize, enforced here too so permission is checked
                // before the request body is read: a STAFF user gets 403, never a validation message.
                .requestMatchers("/api/v1/admin/staff/**", "/api/v1/admin/promo-codes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/v1/admin/products/**", "/api/v1/admin/collections/**", "/api/v1/admin/media/**",
                        "/api/v1/admin/home/**", "/api/v1/admin/care-picks/**", "/api/v1/admin/settings/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/password", "/api/v1/auth/logout").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> errors.write(res, 401, "Please sign in."))
                .accessDeniedHandler((req, res, ex) -> errors.write(res, 403, "You do not have permission to do this.")))
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
