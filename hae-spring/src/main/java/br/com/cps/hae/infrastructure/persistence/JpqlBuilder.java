package br.com.cps.hae.infrastructure.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.Query;

/** Monta cláusulas WHERE dinâmicas sempre com parâmetros nomeados (nunca concatena valores). */
final class JpqlBuilder {

    private final List<String> conditions = new ArrayList<>();
    private final Map<String, Object> parameters = new LinkedHashMap<>();

    JpqlBuilder when(boolean apply, String condition, String name, Object value) {
        if (apply) {
            conditions.add(condition);
            if (name != null) {
                parameters.put(name, value);
            }
        }
        return this;
    }

    JpqlBuilder whenNotNull(Object value, String condition, String name) {
        return when(value != null, condition, name, value);
    }

    String where() {
        return conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);
    }

    <Q extends Query> Q bind(Q query) {
        parameters.forEach(query::setParameter);
        return query;
    }

    void param(String name, Object value) {
        parameters.put(name, value);
    }

    /** Padrão LIKE com curingas escapados. */
    static String likePattern(String text) {
        String escaped = text.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
