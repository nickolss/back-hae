package br.com.cps.hae.web.security;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.cps.hae.web.config.WebProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Segurança HTTP: API stateless autenticada por JWT em cookie HttpOnly, CSRF por double-submit cookie
 * (XSRF-TOKEN), cabeçalhos de segurança e CORS restrito. Autorização fina (papéis e escopo por instituição) é
 * feita nos casos de uso.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(WebProperties.class)
public class SecurityConfiguration {

    private static final String[] PUBLIC_POST = {
            "/api/v1/auth/register", "/api/v1/auth/activate", "/api/v1/auth/login", "/api/v1/auth/logout",
            "/api/v1/auth/password/forgot", "/api/v1/auth/password/reset" };
    private static final String[] PUBLIC_GET = {
            "/api/v1/auth/csrf", "/api/v1/public/**", "/actuator/health", "/actuator/health/**" };
    private static final String[] DOCS = { "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html" };

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, WebProperties properties, JwtDecoder jwtDecoder,
            Clock clock) throws Exception {
        var problems = new ProblemAuthenticationEntryPoint();
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsSource(properties.allowedOrigins())))
                .csrf(csrf -> csrf.spa())
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .logout(l -> l.disable())
                .requestCache(c -> c.disable())
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                        .frameOptions(f -> f.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                        .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=()")))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll();
                    auth.requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll();
                    if (properties.docsPublic()) {
                        auth.requestMatchers(DOCS).permitAll();
                    }
                    auth.requestMatchers("/api/v1/**").authenticated();
                    auth.anyRequest().denyAll();
                })
                .addFilterBefore(sessionFilter(properties, jwtDecoder, problems), AuthorizationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(problems).accessDeniedHandler(problems));

        if (properties.rateLimit().enabled()) {
            http.addFilterBefore(new AuthRateLimitFilter(properties.rateLimit().authPerMinute(), clock),
                    CsrfFilter.class);
        }
        return http.build();
    }

    /**
     * Autenticação pelo JWT do cookie. Montado manualmente (em vez de {@code oauth2ResourceServer()}) porque o
     * DSL do resource server desativa o CSRF para requisições com token — inaceitável quando o token é um cookie
     * enviado automaticamente pelo navegador.
     */
    private static BearerTokenAuthenticationFilter sessionFilter(WebProperties properties, JwtDecoder jwtDecoder,
            ProblemAuthenticationEntryPoint problems) {
        var filter = new BearerTokenAuthenticationFilter(new ProviderManager(new JwtAuthenticationProvider(jwtDecoder)));
        filter.setBearerTokenResolver(new CookieBearerTokenResolver(properties.cookieName()));
        filter.setAuthenticationEntryPoint(problems);
        return filter;
    }

    private static CorsConfigurationSource corsSource(List<String> allowedOrigins) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** Garante que o cookie XSRF-TOKEN seja emitido para o SPA. */
    static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, java.io.IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
