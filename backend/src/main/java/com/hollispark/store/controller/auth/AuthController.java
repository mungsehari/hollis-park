package com.hollispark.store.controller.auth;

import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.auth.LoginRequest;
import com.hollispark.store.dto.auth.Me;
import com.hollispark.store.dto.auth.PasswordChange;
import com.hollispark.store.exception.TooManyRequestsException;
import com.hollispark.store.exception.UnauthorizedException;
import com.hollispark.store.service.user.LoginAttempts;
import com.hollispark.store.service.user.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff sign-in. Sessions live in PostgreSQL (Spring Session) behind an HttpOnly cookie, and every write
 * carries the CSRF token, so no credential is ever readable by page scripts.
 */
@Tag(name = "Auth", description = "Staff sign-in for the admin panel")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager auth;
    private final SecurityContextRepository contexts;
    private final StaffService staff;
    private final LoginAttempts attempts;

    @Operation(summary = "CSRF token; reading it also sets the XSRF-TOKEN cookie. The admin app calls this first")
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> csrf(CsrfToken token) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("headerName", token.getHeaderName(), "token", token.getToken())));
    }

    @Operation(summary = "Sign in. An email is locked for 15 minutes after 5 wrong passwords")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Me>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request,
                                                 HttpServletResponse response) {
        String email = req.email().trim().toLowerCase();
        if (attempts.locked(email)) throw new TooManyRequestsException("Too many attempts. Try again in 15 minutes.");
        Authentication result;
        try {
            result = auth.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, req.password()));
        } catch (AuthenticationException e) {
            attempts.failed(email);
            throw new UnauthorizedException("Email or password is incorrect.");
        }
        attempts.succeeded(email);
        request.getSession(true);
        request.changeSessionId(); // a fresh session id at sign-in prevents session fixation
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return ResponseEntity.ok(ApiResponse.success(staff.signedIn(email), "Signed in"));
    }

    @Operation(summary = "Sign out")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success(null, "Signed out"));
    }

    @Operation(summary = "Who is signed in")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Me>> me(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(staff.me(principal.getName())));
    }

    @Operation(summary = "Change your own password")
    @PostMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChange req, Principal principal) {
        staff.changePassword(principal.getName(), req.current(), req.next());
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed"));
    }
}
