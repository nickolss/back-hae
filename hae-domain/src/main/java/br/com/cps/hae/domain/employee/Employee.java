package br.com.cps.hae.domain.employee;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.EmailAddress;
import br.com.cps.hae.domain.shared.Guard;

/**
 * Funcionário (professor, coordenador, diretor, administrador). A senha é guardada apenas como hash.
 */
public class Employee {

    public static final int NAME_MAX_LENGTH = 100;

    private final UUID id;
    private String name;
    private EmailAddress email;
    private String passwordHash;
    private Role role;
    private UUID institutionId;
    private UUID courseId;
    private final Instant createdAt;
    private final Long version;

    private Employee(UUID id, String name, EmailAddress email, String passwordHash, Role role,
            UUID institutionId, UUID courseId, Instant createdAt, Long version) {
        this.id = Objects.requireNonNull(id);
        this.name = Guard.notBlank(name, "nome", NAME_MAX_LENGTH);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Guard.notNull(passwordHash, "senha");
        this.role = Guard.notNull(role, "papel");
        this.institutionId = institutionId;
        this.courseId = courseId;
        this.createdAt = createdAt;
        this.version = version;
        ensureInstitutionConsistency();
    }

    public static Employee create(String name, EmailAddress email, String passwordHash, Role role,
            UUID institutionId, UUID courseId, Instant now) {
        return new Employee(UUID.randomUUID(), name, email, passwordHash, role, institutionId, courseId, now, null);
    }

    /** Reconstrói a partir da persistência. */
    public static Employee restore(UUID id, String name, String email, String passwordHash, Role role,
            UUID institutionId, UUID courseId, Instant createdAt, Long version) {
        return new Employee(id, name, new EmailAddress(email), passwordHash, role, institutionId, courseId,
                createdAt, version);
    }

    public void rename(String newName) {
        this.name = Guard.notBlank(newName, "nome", NAME_MAX_LENGTH);
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Guard.notNull(newPasswordHash, "senha");
    }

    public void changeRole(Role newRole) {
        this.role = Guard.notNull(newRole, "papel");
        ensureInstitutionConsistency();
    }

    public void assignCourse(UUID newCourseId) {
        this.courseId = newCourseId;
    }

    public boolean belongsTo(UUID otherInstitutionId) {
        return institutionId != null && institutionId.equals(otherInstitutionId);
    }

    private void ensureInstitutionConsistency() {
        if (!role.isGlobal() && institutionId == null) {
            throw new BusinessRuleViolationException("Funcionários não administradores devem pertencer a uma instituição.");
        }
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public EmailAddress email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public UUID institutionId() {
        return institutionId;
    }

    public UUID courseId() {
        return courseId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Long version() {
        return version;
    }
}
