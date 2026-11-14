package br.com.cps.hae.web.api;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.cps.hae.application.hae.HaeView;
import br.com.cps.hae.domain.hae.ClosureReport;
import br.com.cps.hae.domain.hae.Dimension;
import br.com.cps.hae.domain.hae.HaeDetails;
import br.com.cps.hae.domain.hae.HaePeriod;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.hae.Modality;
import br.com.cps.hae.domain.hae.ReviewDecision;
import br.com.cps.hae.domain.hae.TimeRange;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contratos HTTP de HAE. */
final class HaeDtos {

    private HaeDtos() {
    }

    record ScheduleSlot(@NotNull DayOfWeek day, @NotNull LocalTime start, @NotNull LocalTime end) {
    }

    record HaeRequest(
            UUID employeeId,
            @NotNull(message = "Selecione o curso.") UUID courseId,
            @NotBlank(message = "Informe o título.") @Size(max = 200) String title,
            @NotBlank(message = "Descreva o projeto.") @Size(max = 5000) String description,
            @Size(max = 2000) String observations,
            @NotNull(message = "Selecione o tipo.") HaeType type,
            @NotNull(message = "Selecione a modalidade.") Modality modality,
            @NotNull(message = "Selecione a dimensão.") Dimension dimension,
            @NotNull @Min(value = 1, message = "Mínimo de 1 hora.") @Max(value = 40, message = "Máximo de 40 horas.") Integer weeklyHours,
            @NotNull(message = "Informe a data de início.") LocalDate startDate,
            @NotNull(message = "Informe a data de término.") LocalDate endDate,
            @NotEmpty(message = "Informe ao menos um horário.") @Size(max = 7) List<@Valid ScheduleSlot> schedule,
            @Size(max = 100) List<@NotBlank @Size(max = 20) String> studentRas) {

        HaeDetails toDetails() {
            Map<DayOfWeek, TimeRange> map = new EnumMap<>(DayOfWeek.class);
            for (ScheduleSlot slot : schedule) {
                if (map.put(slot.day(), new TimeRange(slot.start(), slot.end())) != null) {
                    throw new BusinessRuleViolationException("Cada dia da semana pode aparecer apenas uma vez.");
                }
            }
            return new HaeDetails(title, description, observations, type, modality, dimension, weeklyHours,
                    new HaePeriod(startDate, endDate), map, studentRas);
        }
    }

    record ReviewRequest(@NotNull(message = "Informe a decisão.") ReviewDecision decision,
            @Size(max = 1000) String comment) {
    }

    record ViewedRequest(@NotNull Boolean viewed) {
    }

    /** Relatório de fechamento: os campos usados dependem de {@code type}. */
    record ClosureRequest(
            @NotNull(message = "Informe o tipo de fechamento.") HaeType type,
            @Size(max = 100) String role,
            @Min(0) @Max(500) Integer studentCount,
            @Size(max = 5000) String studentNames,
            @Size(max = 5000) String approvedStudents,
            @Size(max = 5000) String projectInfo,
            @Size(max = 5000) String studentInfo,
            @Size(max = 100) String supportType,
            @Size(max = 5000) String description,
            @Size(max = 5000) String certificateStudents) {

        ClosureReport toReport() {
            return switch (type) {
                case TCC -> new ClosureReport.Tcc(role, studentCount == null ? 0 : studentCount, studentNames,
                        approvedStudents, projectInfo);
                case ESTAGIO -> new ClosureReport.Internship(studentInfo, approvedStudents);
                case APOIO_DIRECAO -> new ClosureReport.DirectionSupport(supportType, description, approvedStudents,
                        certificateStudents);
            };
        }
    }

    record Ref(UUID id, String name) {

        static Ref of(HaeView.Ref ref) {
            return ref == null ? null : new Ref(ref.id(), ref.name());
        }
    }

    record AttachmentResponse(UUID id, String fileName, String contentType, long sizeBytes, String url,
            Instant uploadedAt) {
    }

    record ClosureResponse(UUID id, HaeType type, String role, Integer studentCount, String studentNames,
            String approvedStudents, String projectInfo, String studentInfo, String supportType, String description,
            String certificateStudents, Instant submittedAt, ReviewDecision decision, Ref reviewer,
            String reviewComment, Instant decidedAt) {

        static ClosureResponse of(HaeView.ClosureView c) {
            var b = new Object() {
                String role, studentNames, approvedStudents, projectInfo, studentInfo, supportType, description,
                        certificateStudents;
                Integer studentCount;
            };
            switch (c.report()) {
                case ClosureReport.Tcc t -> {
                    b.role = t.role();
                    b.studentCount = t.studentCount();
                    b.studentNames = t.studentNames();
                    b.approvedStudents = t.approvedStudents();
                    b.projectInfo = t.projectInfo();
                }
                case ClosureReport.Internship i -> {
                    b.studentInfo = i.studentInfo();
                    b.approvedStudents = i.approvedStudents();
                }
                case ClosureReport.DirectionSupport s -> {
                    b.supportType = s.supportType();
                    b.description = s.description();
                    b.approvedStudents = s.approvedStudents();
                    b.certificateStudents = s.certificateStudents();
                }
            }
            return new ClosureResponse(c.id(), c.report().type(), b.role, b.studentCount, b.studentNames,
                    b.approvedStudents, b.projectInfo, b.studentInfo, b.supportType, b.description,
                    b.certificateStudents, c.submittedAt(), c.decision(), Ref.of(c.reviewer()), c.reviewComment(),
                    c.decidedAt());
        }
    }

    record HaeResponse(UUID id, String title, String description, String observations, HaeType type,
            Modality modality, Dimension dimension, int weeklyHours, LocalDate startDate, LocalDate endDate,
            List<ScheduleSlot> schedule, List<String> studentRas, HaeStatus status, boolean viewed, Ref employee,
            Ref course, Ref institution, Ref reviewer, String reviewComment, List<AttachmentResponse> attachments,
            List<ClosureResponse> closures, HaeView.Permissions permissions, Instant createdAt, Instant updatedAt) {

        static HaeResponse of(HaeView v) {
            var schedule = v.schedule().entrySet().stream()
                    .map(e -> new ScheduleSlot(e.getKey(), e.getValue().start(), e.getValue().end()))
                    .toList();
            var attachments = v.attachments().stream()
                    .map(a -> new AttachmentResponse(a.id(), a.fileName(), a.contentType(), a.sizeBytes(), a.url(),
                            a.uploadedAt()))
                    .toList();
            return new HaeResponse(v.id(), v.title(), v.description(), v.observations(), v.type(), v.modality(),
                    v.dimension(), v.weeklyHours(), v.startDate(), v.endDate(), schedule, v.studentRas(),
                    v.status(), v.viewed(), Ref.of(v.employee()), Ref.of(v.course()), Ref.of(v.institution()),
                    Ref.of(v.reviewer()), v.reviewComment(), attachments,
                    v.closures().stream().map(ClosureResponse::of).toList(), v.permissions(), v.createdAt(),
                    v.updatedAt());
        }
    }
}
