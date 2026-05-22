package com.dataquery.controller;

import com.dataquery.model.User;
import com.dataquery.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /** Estadísticas personales del estudiante autenticado */
    @GetMapping("/my-stats")
    public ResponseEntity<Map<String, Object>> myStats(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getStudentStats(user));
    }

    /** Estadísticas globales — solo PROFESSOR */
    @GetMapping("/global/stats")
    public ResponseEntity<Map<String, Object>> globalStats() {
        return ResponseEntity.ok(dashboardService.getGlobalStats());
    }
}
