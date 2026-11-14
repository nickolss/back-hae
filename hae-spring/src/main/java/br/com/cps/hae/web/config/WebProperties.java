package br.com.cps.hae.web.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param allowedOrigins origens CORS permitidas (em produção o front usa proxy e não precisa de CORS)
 * @param cookieName     nome do cookie de sessão; em produção use o prefixo {@code __Host-}
 * @param cookieSecure   exige HTTPS para o cookie (desligar apenas em desenvolvimento local)
 * @param docsPublic     expõe /swagger-ui e /v3/api-docs sem autenticação
 */
@ConfigurationProperties(prefix = "hae.web")
public record WebProperties(
        @DefaultValue("") List<String> allowedOrigins,
        @DefaultValue("__Host-hae_session") String cookieName,
        @DefaultValue("true") boolean cookieSecure,
        @DefaultValue("false") boolean docsPublic,
        @DefaultValue RateLimit rateLimit) {

    public record RateLimit(@DefaultValue("true") boolean enabled, @DefaultValue("10") int authPerMinute) {
    }
}
