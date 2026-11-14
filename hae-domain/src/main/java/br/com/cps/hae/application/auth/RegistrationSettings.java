package br.com.cps.hae.application.auth;

import java.util.List;

/**
 * Configuração do autocadastro. Toda conta criada por autocadastro recebe o papel PROFESSOR; papéis de gestão
 * são atribuídos apenas por diretores ou administradores.
 *
 * @param allowedEmailDomains domínios aceitos no autocadastro (vazio = qualquer domínio).
 */
public record RegistrationSettings(List<String> allowedEmailDomains) {

    public RegistrationSettings {
        allowedEmailDomains = allowedEmailDomains == null ? List.of()
                : allowedEmailDomains.stream().filter(d -> d != null && !d.isBlank()).map(String::strip).toList();
    }
}
