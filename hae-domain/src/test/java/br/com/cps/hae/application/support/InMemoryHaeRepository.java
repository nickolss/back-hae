package br.com.cps.hae.application.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import br.com.cps.hae.application.port.HaeRepository;
import br.com.cps.hae.domain.hae.Hae;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.shared.Semester;

public class InMemoryHaeRepository implements HaeRepository {

    public final Map<UUID, Hae> store = new LinkedHashMap<>();

    @Override
    public Optional<Hae> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Hae save(Hae hae) {
        store.put(hae.id(), hae);
        return hae;
    }

    @Override
    public void delete(UUID id) {
        store.remove(id);
    }

    @Override
    public int sumWeeklyHours(UUID institutionId, Semester semester, Set<HaeStatus> statuses, UUID excludingHaeId) {
        return store.values().stream()
                .filter(h -> h.institutionId().equals(institutionId))
                .filter(h -> h.semester().equals(semester))
                .filter(h -> statuses.contains(h.status()))
                .filter(h -> !h.id().equals(excludingHaeId))
                .mapToInt(h -> h.details().weeklyHours())
                .sum();
    }

    @Override
    public boolean existsByEmployeeBefore(UUID employeeId, Semester semester, Set<HaeStatus> statuses) {
        return store.values().stream()
                .anyMatch(h -> h.employeeId().equals(employeeId) && h.semester().compareTo(semester) < 0
                        && statuses.contains(h.status()));
    }

    @Override
    public boolean existsByCourse(UUID courseId) {
        return store.values().stream().anyMatch(h -> h.courseId().equals(courseId));
    }

    @Override
    public boolean existsByEmployee(UUID employeeId) {
        return store.values().stream().anyMatch(h -> h.employeeId().equals(employeeId));
    }
}
