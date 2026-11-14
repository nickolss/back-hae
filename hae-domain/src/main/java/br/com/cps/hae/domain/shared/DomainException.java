package br.com.cps.hae.domain.shared;

/**
 * Base de todas as exceções de regra de negócio. Mensagens são seguras para exibição ao usuário final.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
