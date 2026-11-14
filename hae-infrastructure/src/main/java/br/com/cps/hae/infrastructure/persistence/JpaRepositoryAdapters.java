package br.com.cps.hae.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.cps.hae.application.port.CourseRepository;
import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.HaeRepository;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.port.PasswordResetTokenRepository;
import br.com.cps.hae.application.port.PendingRegistrationRepository;
import br.com.cps.hae.domain.auth.PasswordResetToken;
import br.com.cps.hae.domain.auth.PendingRegistration;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.hae.Hae;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.institution.Course;
import br.com.cps.hae.domain.institution.Institution;
import br.com.cps.hae.domain.shared.EmailAddress;
import br.com.cps.hae.domain.shared.Semester;
import br.com.cps.hae.infrastructure.persistence.SpringDataRepositories.CourseJpa;
import br.com.cps.hae.infrastructure.persistence.SpringDataRepositories.EmployeeJpa;
import br.com.cps.hae.infrastructure.persistence.SpringDataRepositories.HaeJpa;
import br.com.cps.hae.infrastructure.persistence.SpringDataRepositories.InstitutionJpa;
import br.com.cps.hae.infrastructure.persistence.SpringDataRepositories.PasswordResetTokenJpa;
import br.com.cps.hae.infrastructure.persistence.SpringDataRepositories.PendingRegistrationJpa;

/** Adaptadores que implementam as portas de persistência da aplicação. */
final class JpaRepositoryAdapters {

    private JpaRepositoryAdapters() {
    }

    @Repository
    @Transactional(readOnly = true)
    static class InstitutionAdapter implements InstitutionRepository {

        private final InstitutionJpa jpa;

        InstitutionAdapter(InstitutionJpa jpa) {
            this.jpa = jpa;
        }

        @Override
        public Optional<Institution> findById(UUID id) {
            return jpa.findById(id).map(PersistenceMapper::toDomain);
        }

        @Override
        public Optional<Institution> findByCode(int code) {
            return jpa.findByCode(code).map(PersistenceMapper::toDomain);
        }

        @Override
        public boolean existsByCode(int code) {
            return jpa.existsByCode(code);
        }

        @Override
        public boolean existsByNameIgnoreCase(String name) {
            return jpa.existsByNameIgnoreCase(name.strip());
        }

        @Override
        public List<Institution> findAll() {
            return jpa.findAll().stream().map(PersistenceMapper::toDomain).toList();
        }

        @Override
        @Transactional
        public Institution save(Institution institution) {
            return PersistenceMapper.toDomain(jpa.save(PersistenceMapper.toEntity(institution)));
        }
    }

    @Repository
    @Transactional(readOnly = true)
    static class CourseAdapter implements CourseRepository {

        private final CourseJpa jpa;

        CourseAdapter(CourseJpa jpa) {
            this.jpa = jpa;
        }

        @Override
        public Optional<Course> findById(UUID id) {
            return jpa.findById(id).map(PersistenceMapper::toDomain);
        }

        @Override
        public List<Course> findByInstitution(UUID institutionId) {
            return jpa.findByInstitutionId(institutionId).stream().map(PersistenceMapper::toDomain).toList();
        }

        @Override
        public boolean existsByInstitutionAndNameIgnoreCase(UUID institutionId, String name) {
            return jpa.existsByInstitutionIdAndNameIgnoreCase(institutionId, name.strip());
        }

        @Override
        @Transactional
        public Course save(Course course) {
            return PersistenceMapper.toDomain(jpa.save(PersistenceMapper.toEntity(course)));
        }

        @Override
        @Transactional
        public void delete(UUID id) {
            jpa.deleteById(id);
        }
    }

    @Repository
    @Transactional(readOnly = true)
    static class EmployeeAdapter implements EmployeeRepository {

        private final EmployeeJpa jpa;

        EmployeeAdapter(EmployeeJpa jpa) {
            this.jpa = jpa;
        }

        @Override
        public Optional<Employee> findById(UUID id) {
            return jpa.findById(id).map(PersistenceMapper::toDomain);
        }

        @Override
        public Optional<Employee> findByEmail(EmailAddress email) {
            return jpa.findByEmail(email.value()).map(PersistenceMapper::toDomain);
        }

        @Override
        public boolean existsByEmail(EmailAddress email) {
            return jpa.existsByEmail(email.value());
        }

        @Override
        public boolean existsByRole(Role role) {
            return jpa.existsByRole(role);
        }

        @Override
        public boolean existsByCourse(UUID courseId) {
            return jpa.existsByCourseId(courseId);
        }

        @Override
        public List<Employee> findByRoleAndCourse(Role role, UUID courseId) {
            return jpa.findByRoleAndCourseId(role, courseId).stream().map(PersistenceMapper::toDomain).toList();
        }

        @Override
        @Transactional
        public Employee save(Employee employee) {
            return PersistenceMapper.toDomain(jpa.save(PersistenceMapper.toEntity(employee)));
        }

        @Override
        @Transactional
        public void delete(UUID id) {
            jpa.deleteById(id);
        }
    }

    @Repository
    @Transactional(readOnly = true)
    static class HaeAdapter implements HaeRepository {

        private static final UUID NONE = new UUID(0, 0);

        private final HaeJpa jpa;

        HaeAdapter(HaeJpa jpa) {
            this.jpa = jpa;
        }

        @Override
        public Optional<Hae> findById(UUID id) {
            return jpa.findById(id).map(PersistenceMapper::toDomain);
        }

        @Override
        @Transactional
        public Hae save(Hae hae) {
            HaeEntity entity = hae.version() == null ? new HaeEntity()
                    : jpa.findById(hae.id()).orElseGet(HaeEntity::new);
            if (hae.version() != null && entity.version != null && !entity.version.equals(hae.version())) {
                throw new org.springframework.orm.ObjectOptimisticLockingFailureException(HaeEntity.class, hae.id());
            }
            PersistenceMapper.copy(hae, entity);
            return PersistenceMapper.toDomain(jpa.saveAndFlush(entity));
        }

        @Override
        @Transactional
        public void delete(UUID id) {
            jpa.deleteById(id);
        }

        @Override
        public int sumWeeklyHours(UUID institutionId, Semester semester, Set<HaeStatus> statuses,
                UUID excludingHaeId) {
            return Math.toIntExact(jpa.sumWeeklyHours(institutionId, semester.firstDay(), semester.lastDay(),
                    statuses, excludingHaeId == null ? NONE : excludingHaeId));
        }

        @Override
        public boolean existsByEmployeeBefore(UUID employeeId, Semester semester, Set<HaeStatus> statuses) {
            return jpa.existsByEmployeeIdAndStartDateBeforeAndStatusIn(employeeId, semester.firstDay(), statuses);
        }

        @Override
        public boolean existsByCourse(UUID courseId) {
            return jpa.existsByCourseId(courseId);
        }

        @Override
        public boolean existsByEmployee(UUID employeeId) {
            return jpa.existsByEmployeeId(employeeId);
        }
    }

    @Repository
    @Transactional
    static class PendingRegistrationAdapter implements PendingRegistrationRepository {

        private final PendingRegistrationJpa jpa;

        PendingRegistrationAdapter(PendingRegistrationJpa jpa) {
            this.jpa = jpa;
        }

        @Override
        public PendingRegistration save(PendingRegistration registration) {
            return PersistenceMapper.toDomain(jpa.save(PersistenceMapper.toEntity(registration)));
        }

        @Override
        public Optional<PendingRegistration> findByTokenHash(String tokenHash) {
            return jpa.findByTokenHash(tokenHash).map(PersistenceMapper::toDomain);
        }

        @Override
        public void deleteByEmail(EmailAddress email) {
            jpa.deleteByEmail(email.value());
        }

        @Override
        public void delete(UUID id) {
            jpa.deleteById(id);
        }

        @Override
        public int deleteExpired(Instant now) {
            return jpa.deleteExpired(now);
        }
    }

    @Repository
    @Transactional
    static class PasswordResetTokenAdapter implements PasswordResetTokenRepository {

        private final PasswordResetTokenJpa jpa;

        PasswordResetTokenAdapter(PasswordResetTokenJpa jpa) {
            this.jpa = jpa;
        }

        @Override
        public PasswordResetToken save(PasswordResetToken token) {
            return PersistenceMapper.toDomain(jpa.save(PersistenceMapper.toEntity(token)));
        }

        @Override
        public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
            return jpa.findByTokenHash(tokenHash).map(PersistenceMapper::toDomain);
        }

        @Override
        public void deleteByEmployee(UUID employeeId) {
            jpa.deleteByEmployeeId(employeeId);
        }

        @Override
        public int deleteExpired(Instant now) {
            return jpa.deleteExpired(now);
        }
    }
}
