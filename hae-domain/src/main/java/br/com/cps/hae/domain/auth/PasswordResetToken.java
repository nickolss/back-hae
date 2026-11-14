package br.com.cps.hae.domain.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Token de redefinição de senha, de uso único e curta duração. Apenas o hash é persistido. */
public record PasswordResetToken(UUID id, UUID employeeId, String tokenHash, Instant expiresAt) {

    public static final Duration TTL = Duration.ofMinutes(30);

    public PasswordResetToken {
        Objects.requireNonNull(id);
        Objects.requireNonNull(employeeId);
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(expiresAt);
    }

    public static PasswordResetToken create(UUID employeeId, String tokenHash, Instant now) {
        return new PasswordResetToken(UUID.randomUUID(), employeeId, tokenHash, now.plus(TTL));
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
