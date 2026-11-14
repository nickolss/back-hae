package br.com.cps.hae.domain.hae;

import java.time.LocalDate;

import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.Semester;

/** Período de vigência da HAE, sempre contido em um único semestre. */
public record HaePeriod(LocalDate start, LocalDate end) {

    public HaePeriod {
        if (start == null || end == null) {
            throw new BusinessRuleViolationException("Datas de início e término são obrigatórias.");
        }
        if (!end.isAfter(start)) {
            throw new BusinessRuleViolationException("A data de término deve ser posterior à data de início.");
        }
        if (!Semester.of(start).equals(Semester.of(end))) {
            throw new BusinessRuleViolationException("A HAE deve começar e terminar no mesmo semestre.");
        }
    }

    public Semester semester() {
        return Semester.of(start);
    }
}
