package br.com.cps.hae.domain.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import br.com.cps.hae.domain.shared.EmailAddress;

/**
 * Cadastro aguardando confirmação de e-mail. Guarda o hash da senha e o hash do token (nunca o token puro).
 */
public record PendingRegistration(
        UUID id,
        String name,
        EmailAddress email,
        String passwordHash,
        UUID institutionId,
        UUID courseId,
        String tokenHash,
        Instant expiresAt) {

    public static final Duration TTL = Duration.ofHours(24);

    public PendingRegistration {
        Objects.requireNonNull(id);
        Objects.requireNonNull(email);
        Objects.requireNonNull(passwordHash);
        Objects.requireNonNull(institutionId);
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(expiresAt);
    }

    public static PendingRegistration create(String name, EmailAddress email, String passwordHash,
            UUID institutionId, UUID courseId, String tokenHash, Instant now) {
        return new PendingRegistration(UUID.randomUUID(), name, email, passwordHash, institutionId, courseId,
                tokenHash, now.plus(TTL));
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
