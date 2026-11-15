package br.com.cps.hae.application.demo;

import br.com.cps.hae.domain.employee.Role;

/** Conta fictícia exibida na tela de login durante a demonstração (sem senha). */
public record DemoAccount(String name, String email, Role role, String description) {
}
