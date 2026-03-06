package br.com.cps.apihae.adapter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cps.apihae.domain.entity.InstitutionCourse;
import br.com.cps.apihae.useCase.Interface.IInstitutionCourseRepository;

public interface InstitutionCourseRepository
        extends IInstitutionCourseRepository, JpaRepository<InstitutionCourse, String> {
}
