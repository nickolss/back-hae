package br.com.cps.hae.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI haeOpenApi(WebProperties properties) {
        return new OpenAPI()
                .info(new Info().title("API do Sistema de HAEs").version("v1")
                        .description("Gestão de Horas de Atividades Específicas — Fatec/CPS."))
                .components(new Components().addSecuritySchemes("session", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name(properties.cookieName())))
                .addSecurityItem(new SecurityRequirement().addList("session"));
    }
}
