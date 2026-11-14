package br.com.cps.hae.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "institution")
class InstitutionEntity {

    @Id
    UUID id;
    int code;
    String name;
    String address;
    @Column(name = "weekly_hours_limit")
    int weeklyHoursLimit;
    boolean active;
    @Column(name = "created_at", updatable = false)
    Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    Instant updatedAt;
    @Version
    Long version;
}
