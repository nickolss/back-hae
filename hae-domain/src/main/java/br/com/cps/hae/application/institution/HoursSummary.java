package br.com.cps.hae.application.institution;

import java.util.UUID;

/** Situação do limite de horas de HAE da instituição no semestre. */
public record HoursSummary(UUID institutionId, String semester, int limit, int allocated, int remaining) {
}
