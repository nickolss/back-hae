package br.com.cps.hae.application.port;

import java.util.Map;
import java.util.UUID;

import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.shared.Semester;

public interface DashboardQueries {

    /** @param institutionId null = todas as instituições */
    Stats stats(UUID institutionId, Semester semester);

    record Stats(long totalHaes, long allocatedWeeklyHours, Map<HaeStatus, Long> byStatus,
            Map<HaeType, Long> byType, Map<String, Long> byCourse, long employees) {
    }
}
