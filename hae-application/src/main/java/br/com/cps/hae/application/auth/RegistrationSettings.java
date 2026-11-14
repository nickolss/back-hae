package br.com.cps.hae.application.auth;

/**
 * Configuração do cadastro.
 *
 * @param coordinatorEmailDomain se preenchido, e-mails desse domínio recebem o papel COORDENADOR ao ativar a
 *                               conta (comportamento legado); vazio desativa a regra.
 * @param allowedEmailDomains    domínios aceitos no autocadastro (vazio = qualquer domínio).
 */
public record RegistrationSettings(String coordinatorEmailDomain, java.util.List<String> allowedEmailDomains) {

    public RegistrationSettings {
        allowedEmailDomains = allowedEmailDomains == null ? java.util.List.of()
                : allowedEmailDomains.stream().filter(d -> d != null && !d.isBlank()).map(String::strip).toList();
    }
}
