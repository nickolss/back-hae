package br.com.cps.hae.web.security;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import br.com.cps.hae.application.account.AccountService;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.AuthenticationFailedException;

/**
 * Converte o JWT validado em {@link Actor}, recarregando o funcionário do banco: mudanças de papel e exclusões
 * têm efeito imediato.
 */
@Component
class CurrentActorArgumentResolver implements HandlerMethodArgumentResolver {

    private final AccountService accounts;

    CurrentActorArgumentResolver(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentActor.class) && parameter.getParameterType() == Actor.class;
    }

    @Override
    public Actor resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new AuthenticationFailedException("Sessão inválida. Faça login novamente.");
        }
        UUID id;
        try {
            id = UUID.fromString(jwt.getToken().getSubject());
        } catch (IllegalArgumentException e) {
            throw new AuthenticationFailedException("Sessão inválida. Faça login novamente.");
        }
        return accounts.loadActor(id)
                .orElseThrow(() -> new AuthenticationFailedException("Sessão inválida. Faça login novamente."));
    }
}
