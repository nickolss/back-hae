package br.com.cps.hae.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import br.com.cps.hae.domain.employee.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "employee")
class EmployeeEntity {

    @Id
    UUID id;
    String name;
    String email;
    @Column(name = "password_hash")
    String passwordHash;
    @Enumerated(EnumType.STRING)
    Role role;
    @Column(name = "institution_id")
    UUID institutionId;
    @Column(name = "course_id")
    UUID courseId;
    @Column(name = "created_at", updatable = false)
    Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    Instant updatedAt;
    @Version
    Long version;
}
