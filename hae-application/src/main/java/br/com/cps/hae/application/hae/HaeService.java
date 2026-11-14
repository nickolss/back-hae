package br.com.cps.hae.application.hae;

import java.time.Clock;
import java.util.UUID;

import br.com.cps.hae.application.port.CourseRepository;
import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.FileStorage;
import br.com.cps.hae.application.port.HaeRepository;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.port.Notifier;
import br.com.cps.hae.application.port.Notifier.HaeNotice;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.hae.Attachment;
import br.com.cps.hae.domain.hae.ClosureReport;
import br.com.cps.hae.domain.hae.Hae;
import br.com.cps.hae.domain.hae.HaeDetails;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.hae.HoursAllocationPolicy;
import br.com.cps.hae.domain.hae.ReviewDecision;
import br.com.cps.hae.domain.institution.Course;
import br.com.cps.hae.domain.institution.Institution;
import br.com.cps.hae.domain.shared.AccessDeniedException;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.NotFoundException;
import br.com.cps.hae.domain.shared.Semester;
import jakarta.transaction.Transactional;

/** Comandos sobre HAEs: ciclo de vida completo. */
@UseCase
@Transactional
public class HaeService {

    private final HaeRepository haes;
    private final EmployeeRepository employees;
    private final InstitutionRepository institutions;
    private final CourseRepository courses;
    private final FileStorage storage;
    private final Notifier notifier;
    private final HaeViewAssembler views;
    private final Clock clock;

    public HaeService(HaeRepository haes, EmployeeRepository employees, InstitutionRepository institutions,
            CourseRepository courses, FileStorage storage, Notifier notifier, HaeViewAssembler views, Clock clock) {
        this.haes = haes;
        this.employees = employees;
        this.institutions = institutions;
        this.courses = courses;
        this.storage = storage;
        this.notifier = notifier;
        this.views = views;
        this.clock = clock;
    }

    /** @param employeeId dono da HAE; null = o próprio usuário. Coordenadores podem criar para professores. */
    public record CreateHaeCommand(UUID employeeId, UUID courseId, HaeDetails details) {
    }

    public HaeView create(Actor actor, CreateHaeCommand command) {
        Employee owner = resolveOwner(actor, command.employeeId());
        if (owner.institutionId() == null) {
            throw new BusinessRuleViolationException("O responsável pela HAE deve pertencer a uma instituição.");
        }
        Course course = loadCourse(command.courseId(), owner.institutionId());
        Semester semester = command.details().period().semester();

        if (haes.existsByEmployeeBefore(owner.id(), semester, HaeStatus.open())) {
            throw new BusinessRuleViolationException(
                    "Existem HAEs de semestres anteriores ainda não concluídas. Finalize-as antes de criar novas.");
        }
        ensureWithinLimit(owner.institutionId(), semester, command.details().weeklyHours(), null);

        Hae hae = haes.save(Hae.create(owner.id(), owner.institutionId(), course.id(), command.details(),
                clock.instant()));
        notifyCoordinators(hae, owner, course, false);
        return views.toView(actor, hae);
    }

    public HaeView update(Actor actor, UUID id, UUID courseId, HaeDetails details) {
        Hae hae = loadVisible(actor, id);
        HaeAccess.require(hae.isOwnedBy(actor.id()));
        Course course = loadCourse(courseId, hae.institutionId());
        ensureWithinLimit(hae.institutionId(), details.period().semester(), details.weeklyHours(), hae.id());
        hae.update(course.id(), details, clock.instant());
        Hae saved = haes.save(hae);
        employees.findById(hae.employeeId()).ifPresent(owner -> notifyCoordinators(saved, owner, course, false));
        return views.toView(actor, saved);
    }

    public void delete(Actor actor, UUID id) {
        Hae hae = loadVisible(actor, id);
        HaeAccess.require(hae.isOwnedBy(actor.id()) || actor.isGlobal());
        hae.ensureDeletable();
        hae.attachments().forEach(a -> storage.delete(a.storageKey()));
        haes.delete(hae.id());
    }

    public HaeView review(Actor actor, UUID id, ReviewDecision decision, String comment) {
        Hae hae = loadVisible(actor, id);
        HaeAccess.require(HaeAccess.canReview(actor, hae));
        hae.review(decision, actor.id(), comment, clock.instant());
        Hae saved = haes.save(hae);
        notifyOwner(saved, comment);
        return views.toView(actor, saved);
    }

    public HaeView markViewed(Actor actor, UUID id, boolean viewed) {
        Hae hae = loadVisible(actor, id);
        HaeAccess.require(HaeAccess.canReview(actor, hae));
        hae.markViewed(viewed);
        return views.toView(actor, haes.save(hae));
    }

    public HaeView requestClosure(Actor actor, UUID id, ClosureReport report) {
        Hae hae = loadVisible(actor, id);
        HaeAccess.require(hae.isOwnedBy(actor.id()));
        hae.requestClosure(report, clock.instant());
        Hae saved = haes.save(hae);
        Course course = courses.findById(saved.courseId()).orElseThrow(() -> NotFoundException.of("Curso"));
        employees.findById(saved.employeeId()).ifPresent(owner -> notifyCoordinators(saved, owner, course, true));
        return views.toView(actor, saved);
    }

    public HaeView reviewClosure(Actor actor, UUID id, ReviewDecision decision, String comment) {
        Hae hae = loadVisible(actor, id);
        HaeAccess.require(HaeAccess.canReview(actor, hae));
        hae.reviewClosure(decision, actor.id(), comment, clock.instant());
        Hae saved = haes.save(hae);
        notifyOwner(saved, comment);
        return views.toView(actor, saved);
    }

    public HaeView addAttachment(Actor actor, UUID id, Attachment attachment) {
        Hae hae = loadVisible(actor, id);
        HaeAccess.require(hae.isOwnedBy(actor.id()));
        hae.addAttachment(attachment, clock.instant());
        return views.toView(actor, haes.save(hae));
    }

    public HaeView removeAttachment(Actor actor, UUID id, UUID attachmentId) {
        Hae hae = loadVisible(actor, id);
        HaeAccess.require(hae.isOwnedBy(actor.id()));
        Attachment removed = hae.removeAttachment(attachmentId, clock.instant());
        Hae saved = haes.save(hae);
        storage.delete(removed.storageKey());
        return views.toView(actor, saved);
    }

    // ------------------------------------------------------------------ apoio

    private Hae loadVisible(Actor actor, UUID id) {
        Hae hae = haes.findById(id).orElseThrow(() -> NotFoundException.of("HAE"));
        if (!HaeAccess.canView(actor, hae)) {
            throw NotFoundException.of("HAE");
        }
        return hae;
    }

    private Employee resolveOwner(Actor actor, UUID employeeId) {
        if (employeeId == null || employeeId.equals(actor.id())) {
            return employees.findById(actor.id()).orElseThrow(() -> NotFoundException.of("Usuário"));
        }
        if (!actor.role().canReviewHaes() && !actor.role().canManageInstitution()) {
            throw new AccessDeniedException("Apenas coordenadores e diretores podem criar HAEs para outros professores.");
        }
        Employee owner = employees.findById(employeeId).orElseThrow(() -> NotFoundException.of("Funcionário"));
        actor.requireInstitutionAccess(owner.institutionId());
        return owner;
    }

    private Course loadCourse(UUID courseId, UUID institutionId) {
        if (courseId == null) {
            throw new BusinessRuleViolationException("O curso é obrigatório.");
        }
        return courses.findById(courseId)
                .filter(c -> c.institutionId().equals(institutionId))
                .orElseThrow(() -> NotFoundException.of("Curso"));
    }

    private void ensureWithinLimit(UUID institutionId, Semester semester, int requested, UUID excludingHaeId) {
        Institution institution = institutions.findById(institutionId)
                .orElseThrow(() -> NotFoundException.of("Instituição"));
        int allocated = haes.sumWeeklyHours(institutionId, semester, HaeStatus.consumingHours(), excludingHaeId);
        HoursAllocationPolicy.ensureWithinLimit(institution.weeklyHoursLimit(), allocated, requested, semester);
    }

    private void notifyCoordinators(Hae hae, Employee owner, Course course, boolean closure) {
        var notice = new HaeNotice(hae.id(), hae.details().title(), owner.name(), course.name());
        employees.findByRoleAndCourse(Role.COORDENADOR, course.id()).stream()
                .filter(coordinator -> !coordinator.id().equals(owner.id()))
                .forEach(coordinator -> {
                    if (closure) {
                        notifier.notifyClosureRequested(coordinator.email(), notice);
                    } else {
                        notifier.notifyHaeSubmitted(coordinator.email(), notice);
                    }
                });
    }

    private void notifyOwner(Hae hae, String comment) {
        employees.findById(hae.employeeId()).ifPresent(owner -> {
            String courseName = courses.findById(hae.courseId()).map(Course::name).orElse("");
            notifier.notifyHaeReviewed(owner.email(),
                    new HaeNotice(hae.id(), hae.details().title(), owner.name(), courseName), hae.status(), comment);
        });
    }
}
