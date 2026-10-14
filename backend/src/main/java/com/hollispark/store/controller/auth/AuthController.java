package com.hollispark.store.controller.auth;

import com.hollispark.store.dto.auth.LoginRequest;
import com.hollispark.store.dto.auth.Me;
import com.hollispark.store.dto.auth.PasswordChange;
import com.hollispark.store.entity.user.StaffUser;
import com.hollispark.store.exception.ApiException;
import com.hollispark.store.repository.user.StaffUserRepository;
import com.hollispark.store.service.user.LoginAttempts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager auth;
    private final SecurityContextRepository contexts;
    private final StaffUserRepository staff;
    private final PasswordEncoder encoder;
    private final LoginAttempts attempts;

    public AuthController(AuthenticationManager auth, SecurityContextRepository contexts, StaffUserRepository staff,
                          PasswordEncoder encoder, LoginAttempts attempts) {
        this.auth = auth;
        this.contexts = contexts;
        this.staff = staff;
        this.encoder = encoder;
        this.attempts = attempts;
    }

    /** The admin app calls this first; reading the token makes Spring set the XSRF-TOKEN cookie. */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/login")
    @Transactional
    public Me login(@Valid @RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        String email = req.email().trim().toLowerCase();
        if (attempts.locked(email)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again in 15 minutes.");
        }
        Authentication result;
        try {
            result = auth.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, req.password()));
        } catch (AuthenticationException e) {
            attempts.failed(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        }
        attempts.succeeded(email);
        request.getSession(true);
        request.changeSessionId(); // a fresh session id at sign-in prevents session fixation
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);

        StaffUser u = staff.findByEmailIgnoreCase(email).orElseThrow();
        u.setLastLoginAt(Instant.now());
        return Me.of(u);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return Map.of("status", "signed-out");
    }

    @GetMapping("/me")
    public Me me(Principal principal) {
        return staff.findByEmailIgnoreCase(principal.getName()).filter(StaffUser::isActive).map(Me::of)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in."));
    }

    @PostMapping("/password")
    @Transactional
    public Map<String, String> changePassword(@Valid @RequestBody PasswordChange req, Principal principal) {
        StaffUser u = staff.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        if (!encoder.matches(req.current(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Your current password is incorrect.");
        }
        u.setPasswordHash(encoder.encode(req.next()));
        return Map.of("status", "changed");
    }
}
