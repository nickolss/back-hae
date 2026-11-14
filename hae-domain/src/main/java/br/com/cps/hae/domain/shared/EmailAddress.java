package br.com.cps.hae.domain.shared;

import java.util.Locale;
import java.util.regex.Pattern;

/** E-mail normalizado (minúsculas, sem espaços). */
public record EmailAddress(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_LENGTH = 254;

    public EmailAddress {
        if (value == null) {
            throw new BusinessRuleViolationException("E-mail é obrigatório.");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new BusinessRuleViolationException("E-mail inválido.");
        }
    }

    public boolean hasDomain(String domain) {
        return value.endsWith("@" + domain.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return value;
    }
}
