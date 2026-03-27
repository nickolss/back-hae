package br.com.cps.apihae.useCase.util;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class CookieUtils {

    public void CreateCookies(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("auth_token", token)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(60L * 60 * 24 * 30)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void DeleteCookies(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("auth_token", "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}