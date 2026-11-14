package br.com.cps.hae.application.auth;

import java.time.Clock;

import br.com.cps.hae.application.port.PasswordResetTokenRepository;
import br.com.cps.hae.application.port.PendingRegistrationRepository;
import br.com.cps.hae.application.shared.UseCase;
import jakarta.transaction.Transactional;

@UseCase
@Transactional
public class ExpiredTokenCleaner {

    private final PendingRegistrationRepository pending;
    private final PasswordResetTokenRepository resetTokens;
    private final Clock clock;

    public ExpiredTokenCleaner(PendingRegistrationRepository pending, PasswordResetTokenRepository resetTokens,
            Clock clock) {
        this.pending = pending;
        this.resetTokens = resetTokens;
        this.clock = clock;
    }

    public int purge() {
        return pending.deleteExpired(clock.instant()) + resetTokens.deleteExpired(clock.instant());
    }
}
