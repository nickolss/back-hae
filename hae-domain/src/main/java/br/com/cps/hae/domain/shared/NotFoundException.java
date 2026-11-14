package br.com.cps.hae.domain.shared;

/** O recurso solicitado não existe ou não está visível para o usuário (HTTP 404). */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String resource) {
        return new NotFoundException(resource + " não encontrado(a).");
    }
}
