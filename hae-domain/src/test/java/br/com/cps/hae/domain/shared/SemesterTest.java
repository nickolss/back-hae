package br.com.cps.hae.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class SemesterTest {

    @Test
    void identificaSemestrePelaData() {
        assertThat(Semester.of(LocalDate.of(2026, 6, 30))).isEqualTo(new Semester(2026, 1));
        assertThat(Semester.of(LocalDate.of(2026, 7, 1))).isEqualTo(new Semester(2026, 2));
    }

    @Test
    void ordenaCronologicamente() {
        assertThat(new Semester(2025, 2)).isLessThan(new Semester(2026, 1));
        assertThat(new Semester(2026, 1)).isLessThan(new Semester(2026, 2));
    }

    @Test
    void emailENormalizado() {
        assertThat(new EmailAddress("  Prof@FATEC.sp.gov.br ").value()).isEqualTo("prof@fatec.sp.gov.br");
        assertThatThrownBy(() -> new EmailAddress("invalido")).isInstanceOf(BusinessRuleViolationException.class);
    }
}
