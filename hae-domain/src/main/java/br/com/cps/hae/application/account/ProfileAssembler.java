package br.com.cps.hae.application.account;

import br.com.cps.hae.application.port.CourseRepository;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.employee.Employee;

/** Monta {@link EmployeeProfile} resolvendo nomes de instituição e curso. */
@UseCase
public class ProfileAssembler {

    private final InstitutionRepository institutions;
    private final CourseRepository courses;

    public ProfileAssembler(InstitutionRepository institutions, CourseRepository courses) {
        this.institutions = institutions;
        this.courses = courses;
    }

    public EmployeeProfile toProfile(Employee employee) {
        var institution = employee.institutionId() == null ? null
                : institutions.findById(employee.institutionId())
                        .map(i -> new EmployeeProfile.Reference(i.id(), i.name(), i.code()))
                        .orElse(null);
        var course = employee.courseId() == null ? null
                : courses.findById(employee.courseId())
                        .map(c -> new EmployeeProfile.Reference(c.id(), c.name(), null))
                        .orElse(null);
        return new EmployeeProfile(employee.id(), employee.name(), employee.email().value(), employee.role(),
                institution, course);
    }
}
