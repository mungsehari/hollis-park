package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.service.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: dashboard")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize(Access.STAFF)
public class AdminDashboardController {
    private final DashboardService dashboard;

    @Operation(summary = "Sales, open work, best sellers and marketplace clicks for the last 30 days")
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardService.Dashboard>> get() {
        return ResponseEntity.ok(ApiResponse.success(dashboard.summary()));
    }
}
