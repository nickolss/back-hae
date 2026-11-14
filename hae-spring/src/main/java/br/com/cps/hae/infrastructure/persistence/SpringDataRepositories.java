package br.com.cps.hae.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.hae.HaeStatus;

/** Repositórios Spring Data (detalhe de implementação, não expostos fora do pacote). */
final class SpringDataRepositories {

    private SpringDataRepositories() {
    }

    interface InstitutionJpa extends JpaRepository<InstitutionEntity, UUID> {

        Optional<InstitutionEntity> findByCode(int code);

        boolean existsByCode(int code);

        boolean existsByNameIgnoreCase(String name);
    }

    interface CourseJpa extends JpaRepository<CourseEntity, UUID> {

        List<CourseEntity> findByInstitutionId(UUID institutionId);

        boolean existsByInstitutionIdAndNameIgnoreCase(UUID institutionId, String name);
    }

    interface EmployeeJpa extends JpaRepository<EmployeeEntity, UUID> {

        Optional<EmployeeEntity> findByEmail(String email);

        boolean existsByEmail(String email);

        boolean existsByRole(Role role);

        boolean existsByCourseId(UUID courseId);

        List<EmployeeEntity> findByRoleAndCourseId(Role role, UUID courseId);
    }

    interface HaeJpa extends JpaRepository<HaeEntity, UUID> {

        @Query("""
                select coalesce(sum(h.weeklyHours), 0) from HaeEntity h
                where h.institutionId = :institutionId
                  and h.startDate between :from and :to
                  and h.status in :statuses
                  and h.id <> :excluding
                """)
        long sumWeeklyHours(@Param("institutionId") UUID institutionId, @Param("from") LocalDate from,
                @Param("to") LocalDate to, @Param("statuses") Collection<HaeStatus> statuses,
                @Param("excluding") UUID excluding);

        boolean existsByEmployeeIdAndStartDateBeforeAndStatusIn(UUID employeeId, LocalDate date,
                Collection<HaeStatus> statuses);

        boolean existsByCourseId(UUID courseId);

        boolean existsByEmployeeId(UUID employeeId);
    }

    interface PendingRegistrationJpa extends JpaRepository<PendingRegistrationEntity, UUID> {

        Optional<PendingRegistrationEntity> findByTokenHash(String tokenHash);

        @Modifying
        @Query("delete from PendingRegistrationEntity p where p.email = :email")
        int deleteByEmail(@Param("email") String email);

        @Modifying
        @Query("delete from PendingRegistrationEntity p where p.expiresAt <= :now")
        int deleteExpired(@Param("now") Instant now);
    }

    interface PasswordResetTokenJpa extends JpaRepository<PasswordResetTokenEntity, UUID> {

        Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

        @Modifying
        @Query("delete from PasswordResetTokenEntity t where t.employeeId = :employeeId")
        int deleteByEmployeeId(@Param("employeeId") UUID employeeId);

        @Modifying
        @Query("delete from PasswordResetTokenEntity t where t.expiresAt <= :now")
        int deleteExpired(@Param("now") Instant now);
    }
}
