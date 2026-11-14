package br.com.cps.hae.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_token")
class PasswordResetTokenEntity {

    @Id
    UUID id;
    @Column(name = "employee_id")
    UUID employeeId;
    @Column(name = "token_hash")
    String tokenHash;
    @Column(name = "expires_at")
    Instant expiresAt;
}
