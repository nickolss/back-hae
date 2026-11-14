package br.com.cps.hae.domain.hae;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.Guard;
import br.com.cps.hae.domain.shared.NotFoundException;
import br.com.cps.hae.domain.shared.Semester;

/**
 * Agregado raiz: Hora de Atividade Específica.
 *
 * <pre>
 * PENDENTE ──aprovar──▶ APROVADO ──solicitar fechamento──▶ FECHAMENTO_SOLICITADO ──aprovar──▶ COMPLETO
 *    │                     ▲                                        │
 *    └──reprovar──▶ REPROVADO                                       └──recusar──▶ APROVADO
 * Edição (PENDENTE | REPROVADO | APROVADO) ⇒ volta a PENDENTE.
 * </pre>
 */
public class Hae {

    public static final int MAX_ATTACHMENTS = 10;
    public static final int REVIEW_COMMENT_MAX = 1_000;

    private final UUID id;
    private final UUID employeeId;
    private final UUID institutionId;
    private UUID courseId;
    private HaeDetails details;
    private HaeStatus status;
    private UUID reviewerId;
    private String reviewComment;
    private boolean viewed;
    private final List<Attachment> attachments;
    private final List<Closure> closures;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    private Hae(Builder b) {
        this.id = Objects.requireNonNull(b.id);
        this.employeeId = Objects.requireNonNull(b.employeeId);
        this.institutionId = Objects.requireNonNull(b.institutionId);
        this.courseId = Objects.requireNonNull(b.courseId);
        this.details = Objects.requireNonNull(b.details);
        this.status = Objects.requireNonNull(b.status);
        this.reviewerId = b.reviewerId;
        this.reviewComment = b.reviewComment;
        this.viewed = b.viewed;
        this.attachments = new ArrayList<>(b.attachments);
        this.closures = new ArrayList<>(b.closures);
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.version = b.version;
    }

    public static Hae create(UUID employeeId, UUID institutionId, UUID courseId, HaeDetails details, Instant now) {
        return builder()
                .id(UUID.randomUUID())
                .employeeId(employeeId)
                .institutionId(institutionId)
                .courseId(courseId)
                .details(details)
                .status(HaeStatus.PENDENTE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    // ---------------------------------------------------------------- comportamento

    public void update(UUID newCourseId, HaeDetails newDetails, Instant now) {
        Guard.state(status.isEditable(), "Esta HAE não pode ser editada no status " + status + ".");
        this.courseId = Objects.requireNonNull(newCourseId);
        this.details = Objects.requireNonNull(newDetails);
        this.status = HaeStatus.PENDENTE;
        this.reviewerId = null;
        this.reviewComment = null;
        this.viewed = false;
        this.updatedAt = now;
    }

    public void review(ReviewDecision decision, UUID reviewer, String comment, Instant now) {
        Guard.state(status == HaeStatus.PENDENTE, "Apenas HAEs pendentes podem ser avaliadas.");
        ensureNotSelfReview(reviewer);
        String normalizedComment = Guard.optional(comment, "comentário", REVIEW_COMMENT_MAX);
        if (decision == ReviewDecision.REJECT && normalizedComment == null) {
            throw new BusinessRuleViolationException("Informe o motivo da reprovação.");
        }
        this.status = decision == ReviewDecision.APPROVE ? HaeStatus.APROVADO : HaeStatus.REPROVADO;
        this.reviewerId = reviewer;
        this.reviewComment = normalizedComment;
        this.viewed = true;
        this.updatedAt = now;
    }

    public void requestClosure(ClosureReport report, Instant now) {
        Guard.state(status == HaeStatus.APROVADO, "Apenas HAEs aprovadas podem solicitar fechamento.");
        Guard.state(report.type() == details.type(),
                "O relatório de fechamento não corresponde ao tipo da HAE (" + details.type() + ").");
        closures.add(Closure.submit(report, now));
        this.status = HaeStatus.FECHAMENTO_SOLICITADO;
        this.viewed = false;
        this.updatedAt = now;
    }

    public void reviewClosure(ReviewDecision decision, UUID reviewer, String comment, Instant now) {
        Guard.state(status == HaeStatus.FECHAMENTO_SOLICITADO, "Não há fechamento pendente para esta HAE.");
        ensureNotSelfReview(reviewer);
        String normalizedComment = Guard.optional(comment, "comentário", REVIEW_COMMENT_MAX);
        if (decision == ReviewDecision.REJECT && normalizedComment == null) {
            throw new BusinessRuleViolationException("Informe o motivo da recusa do fechamento.");
        }
        int index = closures.size() - 1;
        closures.set(index, closures.get(index).decide(decision, reviewer, normalizedComment, now));
        this.status = decision == ReviewDecision.APPROVE ? HaeStatus.COMPLETO : HaeStatus.APROVADO;
        this.reviewerId = reviewer;
        this.reviewComment = normalizedComment;
        this.viewed = true;
        this.updatedAt = now;
    }

    public void markViewed(boolean value) {
        this.viewed = value;
    }

    public void addAttachment(Attachment attachment, Instant now) {
        Guard.state(status == HaeStatus.APROVADO || status == HaeStatus.FECHAMENTO_SOLICITADO,
                "Comprovantes só podem ser anexados a HAEs aprovadas ou em fechamento.");
        Guard.state(attachments.size() < MAX_ATTACHMENTS, "Limite de " + MAX_ATTACHMENTS + " comprovantes atingido.");
        attachments.add(Objects.requireNonNull(attachment));
        this.updatedAt = now;
    }

    public Attachment removeAttachment(UUID attachmentId, Instant now) {
        Guard.state(status != HaeStatus.COMPLETO, "Não é possível remover comprovantes de uma HAE completa.");
        Attachment attachment = attachments.stream()
                .filter(a -> a.id().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> NotFoundException.of("Comprovante"));
        attachments.remove(attachment);
        this.updatedAt = now;
        return attachment;
    }

    public void ensureDeletable() {
        Guard.state(status.isDeletable(), "Apenas HAEs pendentes ou reprovadas podem ser excluídas.");
    }

    public boolean isOwnedBy(UUID candidateEmployeeId) {
        return employeeId.equals(candidateEmployeeId);
    }

    public Semester semester() {
        return details.period().semester();
    }

    public Optional<Closure> pendingClosure() {
        return closures.isEmpty() ? Optional.empty()
                : Optional.of(closures.getLast()).filter(Closure::isPending);
    }

    private void ensureNotSelfReview(UUID reviewer) {
        if (isOwnedBy(reviewer)) {
            throw new BusinessRuleViolationException("Você não pode avaliar a sua própria HAE.");
        }
    }

    // ---------------------------------------------------------------- acesso

    public UUID id() {
        return id;
    }

    public UUID employeeId() {
        return employeeId;
    }

    public UUID institutionId() {
        return institutionId;
    }

    public UUID courseId() {
        return courseId;
    }

    public HaeDetails details() {
        return details;
    }

    public HaeStatus status() {
        return status;
    }

    public UUID reviewerId() {
        return reviewerId;
    }

    public String reviewComment() {
        return reviewComment;
    }

    public boolean viewed() {
        return viewed;
    }

    public List<Attachment> attachments() {
        return Collections.unmodifiableList(attachments);
    }

    public List<Closure> closures() {
        return Collections.unmodifiableList(closures);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Long version() {
        return version;
    }

    // ---------------------------------------------------------------- reconstrução

    public static Builder builder() {
        return new Builder();
    }

    /** Usado pela persistência para reconstruir o agregado. */
    public static final class Builder {
        private UUID id;
        private UUID employeeId;
        private UUID institutionId;
        private UUID courseId;
        private HaeDetails details;
        private HaeStatus status;
        private UUID reviewerId;
        private String reviewComment;
        private boolean viewed;
        private List<Attachment> attachments = List.of();
        private List<Closure> closures = List.of();
        private Instant createdAt;
        private Instant updatedAt;
        private Long version;

        private Builder() {
        }

        public Builder id(UUID value) {
            this.id = value;
            return this;
        }

        public Builder employeeId(UUID value) {
            this.employeeId = value;
            return this;
        }

        public Builder institutionId(UUID value) {
            this.institutionId = value;
            return this;
        }

        public Builder courseId(UUID value) {
            this.courseId = value;
            return this;
        }

        public Builder details(HaeDetails value) {
            this.details = value;
            return this;
        }

        public Builder status(HaeStatus value) {
            this.status = value;
            return this;
        }

        public Builder reviewerId(UUID value) {
            this.reviewerId = value;
            return this;
        }

        public Builder reviewComment(String value) {
            this.reviewComment = value;
            return this;
        }

        public Builder viewed(boolean value) {
            this.viewed = value;
            return this;
        }

        public Builder attachments(List<Attachment> value) {
            this.attachments = value;
            return this;
        }

        public Builder closures(List<Closure> value) {
            this.closures = value;
            return this;
        }

        public Builder createdAt(Instant value) {
            this.createdAt = value;
            return this;
        }

        public Builder updatedAt(Instant value) {
            this.updatedAt = value;
            return this;
        }

        public Builder version(Long value) {
            this.version = value;
            return this;
        }

        public Hae build() {
            return new Hae(this);
        }
    }
}
