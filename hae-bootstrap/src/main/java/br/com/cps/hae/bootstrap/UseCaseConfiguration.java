package br.com.cps.hae.bootstrap;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import br.com.cps.hae.application.auth.RegistrationSettings;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.infrastructure.config.HaeProperties;

/** Registra os casos de uso (classes anotadas com {@link UseCase}) e suas dependências simples. */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "br.com.cps.hae.application", useDefaultFilters = false,
        includeFilters = @Filter(type = FilterType.ANNOTATION, classes = UseCase.class))
class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RegistrationSettings registrationSettings(HaeProperties properties) {
        var registration = properties.registration();
        return registration == null ? new RegistrationSettings("", java.util.List.of())
                : new RegistrationSettings(registration.coordinatorEmailDomain(), registration.allowedEmailDomains());
    }
}
