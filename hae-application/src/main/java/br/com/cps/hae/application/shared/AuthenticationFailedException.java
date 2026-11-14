package br.com.cps.hae.application.shared;

import br.com.cps.hae.domain.shared.DomainException;

/** Credenciais ou token inválidos (HTTP 401). Mensagem genérica para não permitir enumeração. */
public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
