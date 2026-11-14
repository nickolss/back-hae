package br.com.cps.hae.domain.employee;

/** Papéis do sistema. ADMIN e DEV têm acesso global; os demais são restritos à própria instituição. */
public enum Role {
    PROFESSOR,
    COORDENADOR,
    DIRETOR,
    ADMIN,
    DEV;

    public boolean isGlobal() {
        return this == ADMIN || this == DEV;
    }

    /** Pode aprovar/reprovar HAEs e fechamentos. */
    public boolean canReviewHaes() {
        return this == COORDENADOR || isGlobal();
    }

    /** Pode ver todas as HAEs da instituição (não só as próprias). */
    public boolean canViewInstitutionHaes() {
        return this == COORDENADOR || this == DIRETOR || isGlobal();
    }

    /** Pode gerenciar funcionários, cursos e limite de horas da instituição. */
    public boolean canManageInstitution() {
        return this == DIRETOR || isGlobal();
    }

    /** Papéis que este papel pode atribuir a outros usuários. */
    public boolean canAssign(Role target) {
        if (isGlobal()) {
            return true;
        }
        return this == DIRETOR && (target == PROFESSOR || target == COORDENADOR);
    }
}
