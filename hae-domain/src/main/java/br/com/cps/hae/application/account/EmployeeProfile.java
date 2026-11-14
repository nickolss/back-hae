package br.com.cps.hae.application.account;

import java.util.UUID;

import br.com.cps.hae.domain.employee.Role;

/** Dados do usuário expostos à interface (nunca inclui hash de senha). */
public record EmployeeProfile(UUID id, String name, String email, Role role, Reference institution, Reference course) {

    public record Reference(UUID id, String name, Integer code) {
    }
}
