package br.com.cps.apihae.adapter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cps.apihae.domain.entity.Institution;
import br.com.cps.apihae.useCase.Interface.IInstitutionRepository;

public interface InstitutionRepository extends IInstitutionRepository, JpaRepository<Institution, String> {
}
