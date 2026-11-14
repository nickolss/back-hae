package br.com.cps.hae.web.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.Semester;

@Configuration(proxyBeanMethods = false)
class WebMvcConfiguration implements WebMvcConfigurer {

    private final List<HandlerMethodArgumentResolver> resolvers;

    WebMvcConfiguration(List<HandlerMethodArgumentResolver> resolvers) {
        this.resolvers = resolvers;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.addAll(resolvers);
    }

    /** Aceita semestres no formato {@code 2026-2} ou {@code 2026/2}. */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Semester.class, WebMvcConfiguration::parseSemester);
    }

    static Semester parseSemester(String value) {
        String[] parts = value.strip().split("[-/]");
        try {
            return new Semester(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (RuntimeException e) {
            throw new BusinessRuleViolationException("Semestre inválido. Use o formato 2026-2.");
        }
    }
}
