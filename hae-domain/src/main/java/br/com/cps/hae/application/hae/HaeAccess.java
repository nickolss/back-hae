package br.com.cps.hae.application.hae;

import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.domain.hae.Hae;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.shared.AccessDeniedException;

/** Regras de autorização sobre uma HAE específica. */
final class HaeAccess {

    private HaeAccess() {
    }

    static boolean canView(Actor actor, Hae hae) {
        return hae.isOwnedBy(actor.id())
                || (actor.role().canViewInstitutionHaes() && actor.canAccessInstitution(hae.institutionId()));
    }

    static boolean canEdit(Actor actor, Hae hae) {
        return hae.isOwnedBy(actor.id()) && hae.status().isEditable();
    }

    static boolean canDelete(Actor actor, Hae hae) {
        return (hae.isOwnedBy(actor.id()) || actor.isGlobal()) && hae.status().isDeletable();
    }

    static boolean canReview(Actor actor, Hae hae) {
        return actor.role().canReviewHaes() && actor.canAccessInstitution(hae.institutionId())
                && !hae.isOwnedBy(actor.id());
    }

    static boolean canRequestClosure(Actor actor, Hae hae) {
        return hae.isOwnedBy(actor.id()) && hae.status() == HaeStatus.APROVADO;
    }

    static boolean canManageAttachments(Actor actor, Hae hae) {
        return hae.isOwnedBy(actor.id())
                && (hae.status() == HaeStatus.APROVADO || hae.status() == HaeStatus.FECHAMENTO_SOLICITADO);
    }

    static void require(boolean allowed) {
        if (!allowed) {
            throw new AccessDeniedException();
        }
    }
}
