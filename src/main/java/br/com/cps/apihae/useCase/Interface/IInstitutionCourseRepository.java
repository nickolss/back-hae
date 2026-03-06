package br.com.cps.apihae.useCase.Interface;

import java.util.List;
import java.util.Optional;

import br.com.cps.apihae.domain.entity.InstitutionCourse;

public interface IInstitutionCourseRepository {
    List<InstitutionCourse> findByInstitutionIdAndActiveTrueOrderByCourseNameAsc(String institutionId);

    List<InstitutionCourse> findByInstitutionInstitutionCodeAndActiveTrueOrderByCourseNameAsc(Integer institutionCode);

    Optional<InstitutionCourse> findById(String id);

    Optional<InstitutionCourse> findByInstitutionIdAndCourseNameIgnoreCase(String institutionId, String courseName);

    InstitutionCourse save(InstitutionCourse institutionCourse);

    void delete(InstitutionCourse institutionCourse);
}
