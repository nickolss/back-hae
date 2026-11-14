package br.com.cps.hae.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/** Ponto de entrada da infraestrutura: repositórios JPA, propriedades e envio assíncrono. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HaeProperties.class)
@EnableJpaRepositories(basePackages = "br.com.cps.hae.infrastructure.persistence", considerNestedRepositories = true)
@EnableAsync
@ImportRuntimeHints(NativeRuntimeHints.class)
public class InfrastructureConfiguration {
}
