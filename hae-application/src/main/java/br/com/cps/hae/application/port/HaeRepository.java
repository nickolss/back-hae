package br.com.cps.hae.application.port;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import br.com.cps.hae.domain.hae.Hae;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.shared.Semester;

public interface HaeRepository {

    Optional<Hae> findById(UUID id);

    Hae save(Hae hae);

    void delete(UUID id);

    /** Soma das horas semanais das HAEs da instituição no semestre, ignorando {@code excludingHaeId}. */
    int sumWeeklyHours(UUID institutionId, Semester semester, Set<HaeStatus> statuses, UUID excludingHaeId);

    /** Existe HAE do funcionário, iniciada antes do semestre informado, com status em {@code statuses}? */
    boolean existsByEmployeeBefore(UUID employeeId, Semester semester, Set<HaeStatus> statuses);

    boolean existsByCourse(UUID courseId);

    boolean existsByEmployee(UUID employeeId);
}
