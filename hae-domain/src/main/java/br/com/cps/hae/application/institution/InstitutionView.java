package br.com.cps.hae.application.institution;

import java.util.UUID;

import br.com.cps.hae.domain.institution.Institution;

public record InstitutionView(UUID id, int code, String name, String address, int weeklyHoursLimit, boolean active) {

    public static InstitutionView of(Institution i) {
        return new InstitutionView(i.id(), i.code(), i.name(), i.address(), i.weeklyHoursLimit(), i.active());
    }
}
