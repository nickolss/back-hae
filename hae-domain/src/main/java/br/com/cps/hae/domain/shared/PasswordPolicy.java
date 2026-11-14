package br.com.cps.hae.domain.shared;

/**
 * Política de senha (OWASP ASVS 2.1): mínimo 8, máximo 72 (limite do BCrypt), sem regras de composição.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            throw new BusinessRuleViolationException(
                    "A senha deve ter entre " + MIN_LENGTH + " e " + MAX_LENGTH + " caracteres.");
        }
        if (rawPassword.isBlank()) {
            throw new BusinessRuleViolationException("A senha não pode conter apenas espaços.");
        }
    }
}
