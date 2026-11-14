package br.com.cps.hae.domain.hae;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class HaeFixtures {

    private HaeFixtures() {
    }

    public static HaeDetails details(HaeType type, int weeklyHours) {
        return new HaeDetails(
                "Projeto de extensão",
                "Descrição do projeto",
                null,
                type,
                Modality.PRESENCIAL,
                Dimension.DIMENSAO_1_DIDATICO_PEDAGOGICO,
                weeklyHours,
                new HaePeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 10)),
                Map.of(DayOfWeek.MONDAY, new TimeRange(LocalTime.of(8, 0), LocalTime.of(10, 0))),
                List.of("1110482312345"));
    }
}
