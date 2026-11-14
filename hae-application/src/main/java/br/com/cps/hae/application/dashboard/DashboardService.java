package br.com.cps.hae.application.dashboard;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import br.com.cps.hae.application.port.DashboardQueries;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.institution.Institution;
import br.com.cps.hae.domain.shared.AccessDeniedException;
import br.com.cps.hae.domain.shared.Semester;
import jakarta.transaction.Transactional;

@UseCase
@Transactional
public class DashboardService {

    private final DashboardQueries queries;
    private final InstitutionRepository institutions;
    private final Clock clock;

    public DashboardService(DashboardQueries queries, InstitutionRepository institutions, Clock clock) {
        this.queries = queries;
        this.institutions = institutions;
        this.clock = clock;
    }

    public record Dashboard(String semester, Integer weeklyHoursLimit, DashboardQueries.Stats stats) {
    }

    public Dashboard stats(Actor actor, UUID institutionId, Semester semester) {
        if (!actor.role().canViewInstitutionHaes()) {
            throw new AccessDeniedException();
        }
        UUID scope = actor.scopeInstitution(institutionId);
        Semester target = semester != null ? semester : Semester.of(LocalDate.now(clock));
        Integer limit = scope == null ? null
                : institutions.findById(scope).map(Institution::weeklyHoursLimit).orElse(null);
        return new Dashboard(target.toString(), limit, queries.stats(scope, target));
    }
}
