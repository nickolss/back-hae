package br.com.cps.hae.infrastructure.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * Metadados de reflexão para a imagem nativa (GraalVM) que as bibliotecas ainda não fornecem.
 */
class NativeRuntimeHints implements RuntimeHintsRegistrar {

    /** Flyway 12 converte SQLExceptions via reflexão ({@code isFlywaySpecificVersionOf} + construtor). */
    private static final String[] FLYWAY_SQL_EXCEPTIONS = {
            "org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlUnableToConnectToDbException",
            "org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlNoIntegratedAuthException",
            "org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlServerUntrustedCertificateSqlException",
            "org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlNoDriversForInteractiveAuthException" };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Hibernate 7 cria arrays do tipo do id para carregamento em lote (MultiIdEntityLoaderArrayParam).
        hints.reflection().registerType(java.util.UUID[].class);
        for (String type : FLYWAY_SQL_EXCEPTIONS) {
            hints.reflection().registerType(TypeReference.of(type), MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
        }
    }
}
