package br.com.cps.hae.domain.hae;

import java.util.EnumSet;
import java.util.Set;

/** Ciclo de vida de uma HAE. */
public enum HaeStatus {
    PENDENTE,
    APROVADO,
    REPROVADO,
    FECHAMENTO_SOLICITADO,
    COMPLETO;

    private static final Set<HaeStatus> EDITABLE = EnumSet.of(PENDENTE, REPROVADO, APROVADO);
    private static final Set<HaeStatus> DELETABLE = EnumSet.of(PENDENTE, REPROVADO);
    private static final Set<HaeStatus> CONSUMES_HOURS = EnumSet.of(PENDENTE, APROVADO, FECHAMENTO_SOLICITADO, COMPLETO);
    private static final Set<HaeStatus> OPEN = EnumSet.of(PENDENTE, APROVADO, FECHAMENTO_SOLICITADO);

    public boolean isEditable() {
        return EDITABLE.contains(this);
    }

    public boolean isDeletable() {
        return DELETABLE.contains(this);
    }

    /** Conta para o limite de horas da instituição. */
    public boolean consumesHours() {
        return CONSUMES_HOURS.contains(this);
    }

    /** HAE ainda não finalizada (bloqueia novas HAEs em semestres seguintes). */
    public boolean isOpen() {
        return OPEN.contains(this);
    }

    public static Set<HaeStatus> consumingHours() {
        return EnumSet.copyOf(CONSUMES_HOURS);
    }

    public static Set<HaeStatus> open() {
        return EnumSet.copyOf(OPEN);
    }
}
