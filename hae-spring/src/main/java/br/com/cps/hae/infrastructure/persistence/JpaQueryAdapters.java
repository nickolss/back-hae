package br.com.cps.hae.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.cps.hae.application.port.DashboardQueries;
import br.com.cps.hae.application.port.EmployeeQueries;
import br.com.cps.hae.application.port.HaeQueries;
import br.com.cps.hae.application.shared.PageQuery;
import br.com.cps.hae.application.shared.PageResult;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.shared.Semester;
import jakarta.persistence.EntityManager;

/** Consultas de leitura (listagens e indicadores). */
final class JpaQueryAdapters {

    private JpaQueryAdapters() {
    }

    private static <T> PageResult<T> page(List<T> content, PageQuery page, long total) {
        int totalPages = (int) Math.ceil((double) total / page.size());
        return new PageResult<>(content, page.page(), page.size(), total, totalPages);
    }

    @Repository
    @Transactional(readOnly = true)
    static class HaeQueriesAdapter implements HaeQueries {

        private static final String FROM = " from HaeEntity h join EmployeeEntity e on e.id = h.employeeId"
                + " join CourseEntity c on c.id = h.courseId";

        private final EntityManager em;

        HaeQueriesAdapter(EntityManager em) {
            this.em = em;
        }

        @Override
        public PageResult<HaeSummary> search(Criteria criteria, PageQuery page) {
            var where = new JpqlBuilder()
                    .whenNotNull(criteria.institutionId(), "h.institutionId = :institutionId", "institutionId")
                    .whenNotNull(criteria.courseId(), "h.courseId = :courseId", "courseId")
                    .whenNotNull(criteria.employeeId(), "h.employeeId = :employeeId", "employeeId")
                    .whenNotNull(criteria.type(), "h.type = :type", "type")
                    .whenNotNull(criteria.status(), "h.status = :status", "status")
                    .whenNotNull(criteria.viewed(), "h.viewed = :viewed", "viewed");
            if (criteria.semester() != null) {
                where.when(true, "h.startDate between :semFrom and :semTo", "semFrom", criteria.semester().firstDay());
                where.param("semTo", criteria.semester().lastDay());
            }
            if (criteria.text() != null && !criteria.text().isBlank()) {
                where.when(true, "(lower(h.title) like :text escape '\\' or lower(e.name) like :text escape '\\')",
                        "text", JpqlBuilder.likePattern(criteria.text()));
            }

            long total = where.bind(em.createQuery("select count(h)" + FROM + where.where(), Long.class))
                    .getSingleResult();
            List<Object[]> rows = where.bind(em.createQuery("""
                    select h.id, h.title, h.type, h.status, h.weeklyHours, h.startDate, h.endDate, h.viewed,
                           e.id, e.name, c.id, c.name, h.institutionId, h.updatedAt""" + FROM + where.where()
                    + " order by h.updatedAt desc, h.id", Object[].class))
                    .setFirstResult(page.page() * page.size())
                    .setMaxResults(page.size())
                    .getResultList();
            List<HaeSummary> content = rows.stream().map(r -> new HaeSummary((UUID) r[0], (String) r[1],
                    (HaeType) r[2], (HaeStatus) r[3], (Integer) r[4], (LocalDate) r[5], (LocalDate) r[6],
                    (Boolean) r[7], (UUID) r[8], (String) r[9], (UUID) r[10], (String) r[11], (UUID) r[12],
                    (Instant) r[13])).toList();
            return page(content, page, total);
        }
    }

    @Repository
    @Transactional(readOnly = true)
    static class EmployeeQueriesAdapter implements EmployeeQueries {

        private static final String FROM = " from EmployeeEntity e left join InstitutionEntity i on i.id = e.institutionId"
                + " left join CourseEntity c on c.id = e.courseId";

        private final EntityManager em;

        EmployeeQueriesAdapter(EntityManager em) {
            this.em = em;
        }

        @Override
        public PageResult<EmployeeSummary> search(Criteria criteria, PageQuery page) {
            var where = new JpqlBuilder()
                    .whenNotNull(criteria.institutionId(), "e.institutionId = :institutionId", "institutionId")
                    .whenNotNull(criteria.role(), "e.role = :role", "role");
            if (criteria.text() != null && !criteria.text().isBlank()) {
                where.when(true, "(lower(e.name) like :text escape '\\' or lower(e.email) like :text escape '\\')",
                        "text", JpqlBuilder.likePattern(criteria.text()));
            }
            long total = where.bind(em.createQuery("select count(e)" + FROM + where.where(), Long.class))
                    .getSingleResult();
            List<Object[]> rows = where.bind(em.createQuery("""
                    select e.id, e.name, e.email, e.role, e.institutionId, i.name, e.courseId, c.name,
                           (select count(h) from HaeEntity h where h.employeeId = e.id)""" + FROM + where.where()
                    + " order by lower(e.name), e.id", Object[].class))
                    .setFirstResult(page.page() * page.size())
                    .setMaxResults(page.size())
                    .getResultList();
            List<EmployeeSummary> content = rows.stream().map(r -> new EmployeeSummary((UUID) r[0], (String) r[1],
                    (String) r[2], (Role) r[3], (UUID) r[4], (String) r[5], (UUID) r[6], (String) r[7],
                    (Long) r[8])).toList();
            return page(content, page, total);
        }
    }

    @Repository
    @Transactional(readOnly = true)
    static class DashboardQueriesAdapter implements DashboardQueries {

        private final EntityManager em;

        DashboardQueriesAdapter(EntityManager em) {
            this.em = em;
        }

        @Override
        public Stats stats(UUID institutionId, Semester semester) {
            var where = new JpqlBuilder()
                    .whenNotNull(institutionId, "h.institutionId = :institutionId", "institutionId")
                    .when(true, "h.startDate between :semFrom and :semTo", "semFrom", semester.firstDay());
            where.param("semTo", semester.lastDay());

            Map<HaeStatus, Long> byStatus = new EnumMap<>(HaeStatus.class);
            for (HaeStatus s : HaeStatus.values()) {
                byStatus.put(s, 0L);
            }
            grouped(where, "h.status", "", HaeStatus.class).forEach(byStatus::put);

            Map<HaeType, Long> byType = new EnumMap<>(HaeType.class);
            for (HaeType t : HaeType.values()) {
                byType.put(t, 0L);
            }
            grouped(where, "h.type", "", HaeType.class).forEach(byType::put);

            Map<String, Long> byCourse = grouped(where, "c.name", " join CourseEntity c on c.id = h.courseId",
                    String.class);

            long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

            var hoursWhere = new JpqlBuilder()
                    .whenNotNull(institutionId, "h.institutionId = :institutionId", "institutionId")
                    .when(true, "h.startDate between :semFrom and :semTo", "semFrom", semester.firstDay())
                    .when(true, "h.status in :statuses", "statuses", HaeStatus.consumingHours());
            hoursWhere.param("semTo", semester.lastDay());
            long hours = hoursWhere.bind(em.createQuery(
                    "select coalesce(sum(h.weeklyHours), 0) from HaeEntity h" + hoursWhere.where(), Long.class))
                    .getSingleResult();

            var employeesWhere = new JpqlBuilder()
                    .whenNotNull(institutionId, "e.institutionId = :institutionId", "institutionId");
            long employees = employeesWhere.bind(em.createQuery(
                    "select count(e) from EmployeeEntity e" + employeesWhere.where(), Long.class))
                    .getSingleResult();

            return new Stats(total, hours, byStatus, byType, byCourse, employees);
        }

        private <K> Map<K, Long> grouped(JpqlBuilder where, String key, String join, Class<K> keyType) {
            List<Object[]> rows = where.bind(em.createQuery("select " + key + ", count(h) from HaeEntity h" + join
                    + where.where() + " group by " + key + " order by count(h) desc", Object[].class))
                    .getResultList();
            Map<K, Long> result = new LinkedHashMap<>();
            rows.forEach(r -> result.put(keyType.cast(r[0]), (Long) r[1]));
            return result;
        }
    }
}
