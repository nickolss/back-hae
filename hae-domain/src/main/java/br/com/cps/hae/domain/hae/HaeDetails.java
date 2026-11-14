package br.com.cps.hae.domain.hae;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.Guard;

/**
 * Dados descritivos da HAE preenchidos pelo professor. Imutável; validado na criação.
 */
public record HaeDetails(
        String title,
        String description,
        String observations,
        HaeType type,
        Modality modality,
        Dimension dimension,
        int weeklyHours,
        HaePeriod period,
        Map<DayOfWeek, TimeRange> schedule,
        List<String> studentRas) {

    public static final int MIN_WEEKLY_HOURS = 1;
    public static final int MAX_WEEKLY_HOURS = 40;
    public static final int MAX_STUDENTS = 100;
    private static final Pattern RA = Pattern.compile("^[0-9A-Za-z]{5,20}$");

    public HaeDetails {
        title = Guard.notBlank(title, "título", 200);
        description = Guard.notBlank(description, "descrição", 5_000);
        observations = Guard.optional(observations, "observações", 2_000);
        Guard.notNull(type, "tipo");
        Guard.notNull(modality, "modalidade");
        Guard.notNull(dimension, "dimensão");
        weeklyHours = Guard.range(weeklyHours, MIN_WEEKLY_HOURS, MAX_WEEKLY_HOURS, "horas semanais");
        Guard.notNull(period, "período");
        if (schedule == null || schedule.isEmpty()) {
            throw new BusinessRuleViolationException("Informe ao menos um dia no cronograma semanal.");
        }
        schedule = Collections.unmodifiableMap(new EnumMap<>(schedule));
        studentRas = normalizeStudents(type, studentRas);
    }

    private static List<String> normalizeStudents(HaeType type, List<String> ras) {
        if (!type.hasStudents() || ras == null) {
            return List.of();
        }
        List<String> normalized = ras.stream()
                .filter(ra -> ra != null && !ra.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
        if (normalized.size() > MAX_STUDENTS) {
            throw new BusinessRuleViolationException("Máximo de " + MAX_STUDENTS + " alunos por HAE.");
        }
        normalized.forEach(ra -> {
            if (!RA.matcher(ra).matches()) {
                throw new BusinessRuleViolationException("RA inválido: " + ra);
            }
        });
        return normalized;
    }
}
