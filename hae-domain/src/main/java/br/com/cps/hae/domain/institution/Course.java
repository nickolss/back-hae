package br.com.cps.hae.domain.institution;

import java.util.Objects;
import java.util.UUID;

import br.com.cps.hae.domain.shared.Guard;

/** Curso oferecido por uma instituição. */
public record Course(UUID id, UUID institutionId, String name, Long version) {

    public static final int NAME_MAX_LENGTH = 120;

    public Course {
        Objects.requireNonNull(id);
        Objects.requireNonNull(institutionId);
        name = Guard.notBlank(name, "nome do curso", NAME_MAX_LENGTH);
    }

    public static Course create(UUID institutionId, String name) {
        return new Course(UUID.randomUUID(), institutionId, name, null);
    }
}
