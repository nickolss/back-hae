package br.com.cps.hae;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada. Fica no pacote raiz para que a varredura alcance os módulos web e infraestrutura; os casos de
 * uso (sem anotações de framework) são registrados em {@link br.com.cps.hae.bootstrap.UseCaseConfiguration}.
 */
@SpringBootApplication
public class HaeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HaeApplication.class, args);
    }
}
