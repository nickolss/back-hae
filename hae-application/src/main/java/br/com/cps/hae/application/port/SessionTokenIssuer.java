package br.com.cps.hae.application.port;

import java.time.Instant;

import br.com.cps.hae.domain.employee.Employee;

/** Emite o token de sessão (JWT) após autenticação. */
public interface SessionTokenIssuer {

    SessionToken issue(Employee employee);

    record SessionToken(String value, Instant expiresAt) {
    }
}
