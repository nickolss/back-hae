package br.com.cps.hae.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import br.com.cps.hae.domain.auth.PendingRegistration;
import br.com.cps.hae.domain.shared.EmailAddress;

public interface PendingRegistrationRepository {

    PendingRegistration save(PendingRegistration registration);

    Optional<PendingRegistration> findByTokenHash(String tokenHash);

    void deleteByEmail(EmailAddress email);

    void delete(UUID id);

    int deleteExpired(Instant now);
}
