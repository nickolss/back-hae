package br.com.cps.hae.domain.shared;

/** Uma regra de negócio impediu a operação (HTTP 422). */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
