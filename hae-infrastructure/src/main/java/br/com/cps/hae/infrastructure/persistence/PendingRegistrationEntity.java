package br.com.cps.hae.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pending_registration")
class PendingRegistrationEntity {

    @Id
    UUID id;
    String name;
    String email;
    @Column(name = "password_hash")
    String passwordHash;
    @Column(name = "institution_id")
    UUID institutionId;
    @Column(name = "course_id")
    UUID courseId;
    @Column(name = "token_hash")
    String tokenHash;
    @Column(name = "expires_at")
    Instant expiresAt;
}
