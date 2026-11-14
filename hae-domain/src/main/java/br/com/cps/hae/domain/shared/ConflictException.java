package br.com.cps.hae.domain.shared;

/** O estado atual conflita com a operação, ex.: unicidade (HTTP 409). */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
