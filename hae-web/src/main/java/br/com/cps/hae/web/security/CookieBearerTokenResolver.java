package br.com.cps.hae.web.security;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Lê o JWT exclusivamente do cookie HttpOnly (não aceita header Authorization). Ignora o cookie nas rotas
 * públicas para que uma sessão expirada não impeça, por exemplo, um novo login.
 */
class CookieBearerTokenResolver implements BearerTokenResolver {

    private final String cookieName;

    CookieBearerTokenResolver(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/auth/") || uri.startsWith("/api/v1/public/")) {
            return null;
        }
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }
}
