package br.com.cps.hae.application.hae;

import java.util.UUID;

import br.com.cps.hae.application.hae.HaeView.ClosureView;
import br.com.cps.hae.application.hae.HaeView.Permissions;
import br.com.cps.hae.application.hae.HaeView.Ref;
import br.com.cps.hae.application.port.CourseRepository;
import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.hae.Hae;
import br.com.cps.hae.domain.hae.HaeStatus;

@UseCase
public class HaeViewAssembler {

    private final EmployeeRepository employees;
    private final CourseRepository courses;
    private final InstitutionRepository institutions;

    public HaeViewAssembler(EmployeeRepository employees, CourseRepository courses,
            InstitutionRepository institutions) {
        this.employees = employees;
        this.courses = courses;
        this.institutions = institutions;
    }

    public HaeView toView(Actor actor, Hae hae) {
        var d = hae.details();
        boolean canReview = HaeAccess.canReview(actor, hae);
        var permissions = new Permissions(
                HaeAccess.canEdit(actor, hae),
                HaeAccess.canDelete(actor, hae),
                canReview && hae.status() == HaeStatus.PENDENTE,
                HaeAccess.canRequestClosure(actor, hae),
                canReview && hae.status() == HaeStatus.FECHAMENTO_SOLICITADO,
                HaeAccess.canManageAttachments(actor, hae));
        var closures = hae.closures().stream()
                .map(c -> new ClosureView(c.id(), c.report(), c.submittedAt(), c.decision(), employee(c.reviewerId()),
                        c.reviewComment(), c.decidedAt()))
                .toList();
        return new HaeView(hae.id(), d.title(), d.description(), d.observations(), d.type(), d.modality(),
                d.dimension(), d.weeklyHours(), d.period().start(), d.period().end(), d.schedule(), d.studentRas(),
                hae.status(), hae.viewed(), employee(hae.employeeId()), course(hae.courseId()),
                institution(hae.institutionId()), employee(hae.reviewerId()), hae.reviewComment(),
                hae.attachments(), closures, permissions, hae.createdAt(), hae.updatedAt());
    }

    private Ref employee(UUID id) {
        return id == null ? null : employees.findById(id).map(e -> new Ref(e.id(), e.name())).orElse(null);
    }

    private Ref course(UUID id) {
        return courses.findById(id).map(c -> new Ref(c.id(), c.name())).orElse(null);
    }

    private Ref institution(UUID id) {
        return institutions.findById(id).map(i -> new Ref(i.id(), i.name())).orElse(null);
    }
}
