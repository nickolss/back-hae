package br.com.cps.hae.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "course")
class CourseEntity {

    @Id
    UUID id;
    @Column(name = "institution_id")
    UUID institutionId;
    String name;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    Instant updatedAt;
    @Version
    Long version;
}
