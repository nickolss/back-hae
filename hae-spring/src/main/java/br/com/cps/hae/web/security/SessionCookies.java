package br.com.cps.hae.web.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import br.com.cps.hae.application.port.SessionTokenIssuer.SessionToken;
import br.com.cps.hae.web.config.WebProperties;
import jakarta.servlet.http.HttpServletResponse;

/** Cookie de sessão HttpOnly + SameSite=Lax; validade igual à do JWT. */
@Component
public class SessionCookies {

    private final WebProperties properties;

    SessionCookies(WebProperties properties) {
        this.properties = properties;
    }

    public String name() {
        return properties.cookieName();
    }

    public void write(HttpServletResponse response, SessionToken token) {
        long seconds = Math.max(0, Duration.between(Instant.now(), token.expiresAt()).getSeconds());
        response.addHeader(HttpHeaders.SET_COOKIE, build(token.value(), seconds).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", 0).toString());
    }

    private ResponseCookie build(String value, long maxAgeSeconds) {
        return ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
