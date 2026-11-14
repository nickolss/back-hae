package br.com.cps.hae.domain.hae;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import br.com.cps.hae.domain.shared.BusinessRuleViolationException;

class HaeDetailsTest {

    @Test
    void apoioDirecaoDescartaAlunos() {
        assertThat(HaeFixtures.details(HaeType.APOIO_DIRECAO, 2).studentRas()).isEmpty();
    }

    @Test
    void horasForaDoIntervaloSaoRejeitadas() {
        assertThatThrownBy(() -> HaeFixtures.details(HaeType.TCC, 41))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> HaeFixtures.details(HaeType.TCC, 0))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void periodoNaoPodeCruzarSemestres() {
        assertThatThrownBy(() -> new HaePeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 8, 1)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void intervaloDeHorarioDeveSerCrescente() {
        assertThatThrownBy(() -> new TimeRange(LocalTime.of(10, 0), LocalTime.of(9, 0)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void limiteDeHorasDaInstituicao() {
        var semester = new HaePeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)).semester();
        HoursAllocationPolicy.ensureWithinLimit(10, 6, 4, semester);
        assertThatThrownBy(() -> HoursAllocationPolicy.ensureWithinLimit(10, 6, 5, semester))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("restam 4h");
    }
}
