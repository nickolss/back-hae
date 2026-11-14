package br.com.cps.hae.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import br.com.cps.hae.domain.auth.PasswordResetToken;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByEmployee(UUID employeeId);

    int deleteExpired(Instant now);
}
