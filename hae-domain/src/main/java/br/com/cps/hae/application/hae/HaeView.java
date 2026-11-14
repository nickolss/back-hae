package br.com.cps.hae.application.hae;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.cps.hae.domain.hae.Attachment;
import br.com.cps.hae.domain.hae.ClosureReport;
import br.com.cps.hae.domain.hae.Dimension;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.hae.Modality;
import br.com.cps.hae.domain.hae.ReviewDecision;
import br.com.cps.hae.domain.hae.TimeRange;

/** Visão detalhada de uma HAE, incluindo o que o usuário atual pode fazer com ela. */
public record HaeView(
        UUID id,
        String title,
        String description,
        String observations,
        HaeType type,
        Modality modality,
        Dimension dimension,
        int weeklyHours,
        LocalDate startDate,
        LocalDate endDate,
        Map<DayOfWeek, TimeRange> schedule,
        List<String> studentRas,
        HaeStatus status,
        boolean viewed,
        Ref employee,
        Ref course,
        Ref institution,
        Ref reviewer,
        String reviewComment,
        List<Attachment> attachments,
        List<ClosureView> closures,
        Permissions permissions,
        Instant createdAt,
        Instant updatedAt) {

    public record Ref(UUID id, String name) {
    }

    public record ClosureView(UUID id, ClosureReport report, Instant submittedAt, ReviewDecision decision,
            Ref reviewer, String reviewComment, Instant decidedAt) {
    }

    public record Permissions(boolean canEdit, boolean canDelete, boolean canReview, boolean canRequestClosure,
            boolean canReviewClosure, boolean canManageAttachments) {
    }
}
