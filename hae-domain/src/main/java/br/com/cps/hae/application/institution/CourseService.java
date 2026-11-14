package br.com.cps.hae.application.institution;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import br.com.cps.hae.application.port.CourseRepository;
import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.HaeRepository;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.institution.Course;
import br.com.cps.hae.domain.institution.Institution;
import br.com.cps.hae.domain.shared.ConflictException;
import br.com.cps.hae.domain.shared.NotFoundException;
import jakarta.transaction.Transactional;

@UseCase
@Transactional
public class CourseService {

    private final CourseRepository courses;
    private final InstitutionRepository institutions;
    private final HaeRepository haes;
    private final EmployeeRepository employees;

    public CourseService(CourseRepository courses, InstitutionRepository institutions, HaeRepository haes,
            EmployeeRepository employees) {
        this.courses = courses;
        this.institutions = institutions;
        this.haes = haes;
        this.employees = employees;
    }

    public record CourseView(UUID id, UUID institutionId, String name) {

        static CourseView of(Course c) {
            return new CourseView(c.id(), c.institutionId(), c.name());
        }
    }

    /** Lista pública de cursos de uma unidade ativa (tela de cadastro). */
    public List<CourseView> listPublic(int institutionCode) {
        Institution institution = institutions.findByCode(institutionCode)
                .filter(Institution::active)
                .orElseThrow(() -> NotFoundException.of("Instituição"));
        return sorted(courses.findByInstitution(institution.id()));
    }

    public List<CourseView> list(Actor actor, UUID institutionId) {
        actor.requireInstitutionAccess(institutionId);
        return sorted(courses.findByInstitution(institutionId));
    }

    public CourseView add(Actor actor, UUID institutionId, String name) {
        actor.requireInstitutionManager(institutionId);
        institutions.findById(institutionId).orElseThrow(() -> NotFoundException.of("Instituição"));
        Course course = Course.create(institutionId, name);
        if (courses.existsByInstitutionAndNameIgnoreCase(institutionId, course.name())) {
            throw new ConflictException("Curso já cadastrado nesta instituição.");
        }
        return CourseView.of(courses.save(course));
    }

    public void remove(Actor actor, UUID institutionId, UUID courseId) {
        actor.requireInstitutionManager(institutionId);
        Course course = courses.findById(courseId)
                .filter(c -> c.institutionId().equals(institutionId))
                .orElseThrow(() -> NotFoundException.of("Curso"));
        if (haes.existsByCourse(course.id()) || employees.existsByCourse(course.id())) {
            throw new ConflictException("O curso possui HAEs ou funcionários vinculados e não pode ser removido.");
        }
        courses.delete(course.id());
    }

    private static List<CourseView> sorted(List<Course> list) {
        return list.stream().sorted(Comparator.comparing(Course::name)).map(CourseView::of).toList();
    }
}
