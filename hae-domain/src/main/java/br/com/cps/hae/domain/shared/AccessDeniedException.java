package br.com.cps.hae.domain.shared;

/** O usuário autenticado não tem permissão para a operação (HTTP 403). */
public class AccessDeniedException extends DomainException {

    public AccessDeniedException() {
        super("Você não tem permissão para realizar esta operação.");
    }

    public AccessDeniedException(String message) {
        super(message);
    }
}
