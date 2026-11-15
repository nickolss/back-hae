package br.com.cps.hae.infrastructure.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configurações da aplicação (prefixo {@code hae}). Segredos vêm de variáveis de ambiente. */
@ConfigurationProperties(prefix = "hae")
public record HaeProperties(Jwt jwt, Mail mail, Cloudinary cloudinary, Registration registration, Admin admin,
        Demo demo) {

    public record Jwt(String secret, @DefaultValue("PT2H") Duration ttl, @DefaultValue("hae-api") String issuer) {
    }

    /**
     * @param from        remetente
     * @param feedbackTo  caixa que recebe mensagens de "Fale conosco"
     * @param frontendUrl URL pública do front, usada nos links dos e-mails
     */
    public record Mail(String from, String feedbackTo, String frontendUrl) {
    }

    public record Cloudinary(String cloudName, String apiKey, String apiSecret) {

        public boolean configured() {
            return notBlank(cloudName) && notBlank(apiKey) && notBlank(apiSecret);
        }
    }

    public record Registration(@DefaultValue("") List<String> allowedEmailDomains) {
    }

    public record Admin(String name, String email, String password) {
    }

    /**
     * Modo demonstração: popula o banco com dados fictícios. A senha das contas de demonstração vem de variável de
     * ambiente (nunca do código).
     */
    public record Demo(@DefaultValue("false") boolean enabled, String password) {
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
