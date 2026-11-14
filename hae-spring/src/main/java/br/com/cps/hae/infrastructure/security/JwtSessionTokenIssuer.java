package br.com.cps.hae.infrastructure.security;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import br.com.cps.hae.application.port.SessionTokenIssuer;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.infrastructure.config.HaeProperties;

/** JWT mínimo: apenas o id do funcionário (o papel é recarregado do banco a cada requisição). */
@Component
class JwtSessionTokenIssuer implements SessionTokenIssuer {

    private final JwtEncoder encoder;
    private final HaeProperties properties;
    private final Clock clock;

    JwtSessionTokenIssuer(JwtEncoder encoder, HaeProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public SessionToken issue(Employee employee) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.jwt().ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(employee.id().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(java.util.UUID.randomUUID().toString())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new SessionToken(token, expiresAt);
    }
}
