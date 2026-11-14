package br.com.cps.hae.infrastructure.persistence;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import br.com.cps.hae.domain.auth.PasswordResetToken;
import br.com.cps.hae.domain.auth.PendingRegistration;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.hae.Attachment;
import br.com.cps.hae.domain.hae.Closure;
import br.com.cps.hae.domain.hae.ClosureReport;
import br.com.cps.hae.domain.hae.Hae;
import br.com.cps.hae.domain.hae.HaeDetails;
import br.com.cps.hae.domain.hae.HaePeriod;
import br.com.cps.hae.domain.hae.TimeRange;
import br.com.cps.hae.domain.institution.Course;
import br.com.cps.hae.domain.institution.Institution;
import br.com.cps.hae.domain.shared.EmailAddress;

/** Conversão entre agregados de domínio e entidades JPA. */
final class PersistenceMapper {

    private PersistenceMapper() {
    }

    // --------------------------------------------------------------- Institution

    static InstitutionEntity toEntity(Institution d) {
        var e = new InstitutionEntity();
        e.id = d.id();
        e.code = d.code();
        e.name = d.name();
        e.address = d.address();
        e.weeklyHoursLimit = d.weeklyHoursLimit();
        e.active = d.active();
        e.createdAt = d.createdAt();
        e.version = d.version();
        return e;
    }

    static Institution toDomain(InstitutionEntity e) {
        return Institution.restore(e.id, e.code, e.name, e.address, e.weeklyHoursLimit, e.active, e.createdAt,
                e.version);
    }

    // --------------------------------------------------------------- Course

    static CourseEntity toEntity(Course d) {
        var e = new CourseEntity();
        e.id = d.id();
        e.institutionId = d.institutionId();
        e.name = d.name();
        e.version = d.version();
        return e;
    }

    static Course toDomain(CourseEntity e) {
        return new Course(e.id, e.institutionId, e.name, e.version);
    }

    // --------------------------------------------------------------- Employee

    static EmployeeEntity toEntity(Employee d) {
        var e = new EmployeeEntity();
        e.id = d.id();
        e.name = d.name();
        e.email = d.email().value();
        e.passwordHash = d.passwordHash();
        e.role = d.role();
        e.institutionId = d.institutionId();
        e.courseId = d.courseId();
        e.createdAt = d.createdAt();
        e.version = d.version();
        return e;
    }

    static Employee toDomain(EmployeeEntity e) {
        return Employee.restore(e.id, e.name, e.email, e.passwordHash, e.role, e.institutionId, e.courseId,
                e.createdAt, e.version);
    }

    // --------------------------------------------------------------- Auth tokens

    static PendingRegistrationEntity toEntity(PendingRegistration d) {
        var e = new PendingRegistrationEntity();
        e.id = d.id();
        e.name = d.name();
        e.email = d.email().value();
        e.passwordHash = d.passwordHash();
        e.institutionId = d.institutionId();
        e.courseId = d.courseId();
        e.tokenHash = d.tokenHash();
        e.expiresAt = d.expiresAt();
        return e;
    }

    static PendingRegistration toDomain(PendingRegistrationEntity e) {
        return new PendingRegistration(e.id, e.name, new EmailAddress(e.email), e.passwordHash, e.institutionId,
                e.courseId, e.tokenHash, e.expiresAt);
    }

    static PasswordResetTokenEntity toEntity(PasswordResetToken d) {
        var e = new PasswordResetTokenEntity();
        e.id = d.id();
        e.employeeId = d.employeeId();
        e.tokenHash = d.tokenHash();
        e.expiresAt = d.expiresAt();
        return e;
    }

    static PasswordResetToken toDomain(PasswordResetTokenEntity e) {
        return new PasswordResetToken(e.id, e.employeeId, e.tokenHash, e.expiresAt);
    }

    // --------------------------------------------------------------- Hae

    /** Copia o estado do agregado para a entidade (nova ou gerenciada). */
    static void copy(Hae d, HaeEntity e) {
        var details = d.details();
        e.id = d.id();
        e.employeeId = d.employeeId();
        e.institutionId = d.institutionId();
        e.courseId = d.courseId();
        e.title = details.title();
        e.description = details.description();
        e.observations = details.observations();
        e.type = details.type();
        e.modality = details.modality();
        e.dimension = details.dimension();
        e.weeklyHours = details.weeklyHours();
        e.startDate = details.period().start();
        e.endDate = details.period().end();
        e.status = d.status();
        e.reviewerId = d.reviewerId();
        e.reviewComment = d.reviewComment();
        e.viewed = d.viewed();
        e.createdAt = d.createdAt();
        e.updatedAt = d.updatedAt();

        e.schedule.clear();
        details.schedule().forEach((day, range) -> {
            var slot = new HaeEntity.Slot();
            slot.start = range.start();
            slot.end = range.end();
            e.schedule.put(day, slot);
        });

        e.students.clear();
        e.students.addAll(details.studentRas());

        e.attachments.clear();
        d.attachments().forEach(a -> {
            var att = new HaeEntity.AttachmentEmbeddable();
            att.id = a.id();
            att.fileName = a.fileName();
            att.contentType = a.contentType();
            att.sizeBytes = a.sizeBytes();
            att.url = a.url();
            att.storageKey = a.storageKey();
            att.uploadedAt = a.uploadedAt();
            e.attachments.add(att);
        });

        Map<java.util.UUID, ClosureEntity> existing = new java.util.HashMap<>();
        e.closures.forEach(c -> existing.put(c.id, c));
        List<ClosureEntity> closures = new ArrayList<>();
        List<Closure> domainClosures = d.closures();
        for (int i = 0; i < domainClosures.size(); i++) {
            Closure c = domainClosures.get(i);
            ClosureEntity ce = existing.getOrDefault(c.id(), new ClosureEntity());
            copyClosure(c, ce, i);
            ce.hae = e;
            closures.add(ce);
        }
        e.closures.clear();
        e.closures.addAll(closures);
    }

    private static void copyClosure(Closure c, ClosureEntity e, int position) {
        e.id = c.id();
        e.position = position;
        e.submittedAt = c.submittedAt();
        e.decision = c.decision();
        e.reviewerId = c.reviewerId();
        e.reviewComment = c.reviewComment();
        e.decidedAt = c.decidedAt();
        e.reportType = c.report().type();
        switch (c.report()) {
            case ClosureReport.Tcc t -> {
                e.role = t.role();
                e.studentCount = t.studentCount();
                e.studentNames = t.studentNames();
                e.approvedStudents = t.approvedStudents();
                e.projectInfo = t.projectInfo();
            }
            case ClosureReport.Internship i -> {
                e.studentInfo = i.studentInfo();
                e.approvedStudents = i.approvedStudents();
            }
            case ClosureReport.DirectionSupport s -> {
                e.supportType = s.supportType();
                e.description = s.description();
                e.approvedStudents = s.approvedStudents();
                e.certificateStudents = s.certificateStudents();
            }
        }
    }

    static Hae toDomain(HaeEntity e) {
        Map<DayOfWeek, TimeRange> schedule = new EnumMap<>(DayOfWeek.class);
        e.schedule.forEach((day, slot) -> schedule.put(day, new TimeRange(slot.start, slot.end)));
        var details = new HaeDetails(e.title, e.description, e.observations, e.type, e.modality, e.dimension,
                e.weeklyHours, new HaePeriod(e.startDate, e.endDate), schedule, List.copyOf(e.students));
        var attachments = e.attachments.stream()
                .map(a -> new Attachment(a.id, a.fileName, a.contentType, a.sizeBytes, a.url, a.storageKey,
                        a.uploadedAt))
                .toList();
        var closures = e.closures.stream()
                .map(c -> new Closure(c.id, report(c), c.submittedAt, c.decision, c.reviewerId, c.reviewComment,
                        c.decidedAt))
                .toList();
        return Hae.builder()
                .id(e.id)
                .employeeId(e.employeeId)
                .institutionId(e.institutionId)
                .courseId(e.courseId)
                .details(details)
                .status(e.status)
                .reviewerId(e.reviewerId)
                .reviewComment(e.reviewComment)
                .viewed(e.viewed)
                .attachments(attachments)
                .closures(closures)
                .createdAt(e.createdAt)
                .updatedAt(e.updatedAt)
                .version(e.version)
                .build();
    }

    private static ClosureReport report(ClosureEntity c) {
        return switch (c.reportType) {
            case TCC -> new ClosureReport.Tcc(c.role, c.studentCount == null ? 0 : c.studentCount, c.studentNames,
                    c.approvedStudents, c.projectInfo);
            case ESTAGIO -> new ClosureReport.Internship(c.studentInfo, c.approvedStudents);
            case APOIO_DIRECAO -> new ClosureReport.DirectionSupport(c.supportType, c.description,
                    c.approvedStudents, c.certificateStudents);
        };
    }
}
