package br.com.cps.hae.domain.shared;

import java.util.Objects;

/** Validações de invariantes usadas pelos objetos de domínio. */
public final class Guard {

    private Guard() {
    }

    public static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new BusinessRuleViolationException("O campo '" + field + "' é obrigatório.");
        }
        return value;
    }

    public static String notBlank(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException("O campo '" + field + "' é obrigatório.");
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new BusinessRuleViolationException(
                    "O campo '" + field + "' deve ter no máximo " + maxLength + " caracteres.");
        }
        return trimmed;
    }

    public static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return notBlank(value, field, maxLength);
    }

    public static int range(Integer value, int min, int max, String field) {
        Objects.requireNonNull(field);
        if (value == null || value < min || value > max) {
            throw new BusinessRuleViolationException(
                    "O campo '" + field + "' deve estar entre " + min + " e " + max + ".");
        }
        return value;
    }

    public static void state(boolean condition, String message) {
        if (!condition) {
            throw new BusinessRuleViolationException(message);
        }
    }
}
