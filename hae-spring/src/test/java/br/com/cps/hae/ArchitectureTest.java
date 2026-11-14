package br.com.cps.hae;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Garante a regra de dependência da Clean Architecture entre os módulos. */
@AnalyzeClasses(packages = "br.com.cps.hae", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("br.com.cps.hae.domain..")
            .layer("Application").definedBy("br.com.cps.hae.application..")
            .layer("Infrastructure").definedBy("br.com.cps.hae.infrastructure..")
            .layer("Web").definedBy("br.com.cps.hae.web..")
            .layer("Bootstrap").definedBy("br.com.cps.hae.bootstrap..", "br.com.cps.hae")
            .whereLayer("Bootstrap").mayNotBeAccessedByAnyLayer()
            .whereLayer("Web").mayOnlyBeAccessedByLayers("Bootstrap")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Bootstrap")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Infrastructure", "Bootstrap")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Web", "Infrastructure", "Bootstrap");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses().that().resideInAPackage("br.com.cps.hae.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "lombok..",
                    "com.fasterxml..", "tools.jackson..", "org.hibernate..");

    @ArchTest
    static final ArchRule applicationIsFrameworkFree = noClasses().that()
            .resideInAPackage("br.com.cps.hae.application..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..",
                    "jakarta.servlet..", "org.hibernate..");

    @ArchTest
    static final ArchRule webDoesNotTouchInfrastructure = noClasses().that().resideInAPackage("br.com.cps.hae.web..")
            .should().dependOnClassesThat().resideInAPackage("br.com.cps.hae.infrastructure..");
}
