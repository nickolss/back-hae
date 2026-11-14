package br.com.cps.hae.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.cps.hae.domain.institution.Institution;

public interface InstitutionRepository {

    Optional<Institution> findById(UUID id);

    Optional<Institution> findByCode(int code);

    boolean existsByCode(int code);

    boolean existsByNameIgnoreCase(String name);

    List<Institution> findAll();

    Institution save(Institution institution);
}
