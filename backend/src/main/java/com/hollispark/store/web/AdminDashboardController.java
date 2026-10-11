package com.hollispark.store.web;

import com.hollispark.store.service.DashboardService;
import com.hollispark.store.service.DashboardService.Dashboard;
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
