package br.com.cps.hae.web.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.cps.hae.application.dashboard.DashboardService;
import br.com.cps.hae.application.dashboard.DashboardService.Dashboard;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.domain.shared.Semester;
import br.com.cps.hae.web.security.CurrentActor;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Indicadores")
class DashboardController {

    private final DashboardService dashboard;

    DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    Dashboard stats(@Parameter(hidden = true) @CurrentActor Actor actor,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) Semester semester) {
        return dashboard.stats(actor, institutionId, semester);
    }
}
