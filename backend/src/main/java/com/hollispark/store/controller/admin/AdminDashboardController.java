package com.hollispark.store.controller.admin;

import com.hollispark.store.service.dashboard.DashboardService.Dashboard;
import com.hollispark.store.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final DashboardService dashboard;

    @GetMapping
    public Dashboard get() {
        return dashboard.summary();
    }
}
