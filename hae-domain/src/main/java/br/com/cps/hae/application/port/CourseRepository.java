package br.com.cps.hae.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.cps.hae.domain.institution.Course;

public interface CourseRepository {

    Optional<Course> findById(UUID id);

    List<Course> findByInstitution(UUID institutionId);

    boolean existsByInstitutionAndNameIgnoreCase(UUID institutionId, String name);

    Course save(Course course);

    void delete(UUID id);
}
