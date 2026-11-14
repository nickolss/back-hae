package br.com.cps.hae.application.shared;

import java.util.Objects;
import java.util.UUID;

import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.shared.AccessDeniedException;

/**
 * Usuário autenticado que executa um caso de uso. Centraliza as verificações de escopo por instituição.
 */
public record Actor(UUID id, String name, String email, Role role, UUID institutionId, UUID courseId) {

    public Actor {
        Objects.requireNonNull(id);
        Objects.requireNonNull(role);
    }

    public static Actor of(Employee employee) {
        return new Actor(employee.id(), employee.name(), employee.email().value(), employee.role(),
                employee.institutionId(), employee.courseId());
    }

    public boolean isGlobal() {
        return role.isGlobal();
    }

    public boolean canAccessInstitution(UUID targetInstitutionId) {
        return isGlobal() || (institutionId != null && institutionId.equals(targetInstitutionId));
    }

    public void requireInstitutionAccess(UUID targetInstitutionId) {
        if (!canAccessInstitution(targetInstitutionId)) {
            throw new AccessDeniedException();
        }
    }

    public void requireGlobal() {
        if (!isGlobal()) {
            throw new AccessDeniedException();
        }
    }

    public void requireInstitutionManager(UUID targetInstitutionId) {
        if (!role.canManageInstitution()) {
            throw new AccessDeniedException();
        }
        requireInstitutionAccess(targetInstitutionId);
    }

    /**
     * Instituição efetiva para consultas: usuários globais podem escolher (ou null = todas); os demais ficam
     * restritos à própria.
     */
    public UUID scopeInstitution(UUID requested) {
        return isGlobal() ? requested : institutionId;
    }
}
