package br.com.cps.hae.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.cps.hae.application.shared.PageQuery;
import br.com.cps.hae.application.shared.PageResult;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.shared.Semester;

/** Consultas de leitura otimizadas (CQRS leve) para listagens de HAEs. */
public interface HaeQueries {

    PageResult<HaeSummary> search(Criteria criteria, PageQuery page);

    record Criteria(UUID institutionId, UUID courseId, UUID employeeId, HaeType type, HaeStatus status,
            Boolean viewed, Semester semester, String text) {
    }

    record HaeSummary(UUID id, String title, HaeType type, HaeStatus status, int weeklyHours, LocalDate startDate,
            LocalDate endDate, boolean viewed, UUID employeeId, String employeeName, UUID courseId,
            String courseName, UUID institutionId, Instant updatedAt) {
    }
}
