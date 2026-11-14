package br.com.cps.hae.domain.hae;

import java.time.Duration;
import java.time.LocalTime;

import br.com.cps.hae.domain.shared.BusinessRuleViolationException;

/** Intervalo de horário dentro de um dia. */
public record TimeRange(LocalTime start, LocalTime end) {

    public TimeRange {
        if (start == null || end == null) {
            throw new BusinessRuleViolationException("Horário de início e fim são obrigatórios.");
        }
        if (!end.isAfter(start)) {
            throw new BusinessRuleViolationException("O horário de término deve ser posterior ao de início.");
        }
    }

    public Duration duration() {
        return Duration.between(start, end);
    }
}
