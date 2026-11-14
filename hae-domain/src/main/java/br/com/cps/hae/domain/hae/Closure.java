package br.com.cps.hae.domain.hae;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma solicitação de fechamento e sua decisão. O histórico de fechamentos de uma HAE é preservado.
 */
public record Closure(
        UUID id,
        ClosureReport report,
        Instant submittedAt,
        ReviewDecision decision,
        UUID reviewerId,
        String reviewComment,
        Instant decidedAt) {

    public Closure {
        Objects.requireNonNull(id);
        Objects.requireNonNull(report);
        Objects.requireNonNull(submittedAt);
    }

    static Closure submit(ClosureReport report, Instant now) {
        return new Closure(UUID.randomUUID(), report, now, null, null, null, null);
    }

    boolean isPending() {
        return decision == null;
    }

    Closure decide(ReviewDecision newDecision, UUID reviewer, String comment, Instant now) {
        return new Closure(id, report, submittedAt, newDecision, reviewer, comment, now);
    }
}
