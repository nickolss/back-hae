package br.com.cps.hae.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import br.com.cps.hae.application.auth.ExpiredTokenCleaner;
import br.com.cps.hae.application.auth.InitialAdminProvisioner;
import br.com.cps.hae.application.demo.DemoDataSeeder;
import br.com.cps.hae.application.demo.DemoSettings;
import br.com.cps.hae.infrastructure.config.HaeProperties;

/** Tarefas de inicialização e manutenção periódica. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class StartupTasks implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupTasks.class);

    private final InitialAdminProvisioner adminProvisioner;
    private final ExpiredTokenCleaner tokenCleaner;
    private final DemoDataSeeder demoSeeder;
    private final DemoSettings demoSettings;
    private final HaeProperties properties;

    StartupTasks(InitialAdminProvisioner adminProvisioner, ExpiredTokenCleaner tokenCleaner, DemoDataSeeder demoSeeder,
            DemoSettings demoSettings, HaeProperties properties) {
        this.adminProvisioner = adminProvisioner;
        this.tokenCleaner = tokenCleaner;
        this.demoSeeder = demoSeeder;
        this.demoSettings = demoSettings;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        var admin = properties.admin();
        if (admin != null) {
            adminProvisioner.provision(admin.name(), admin.email(), admin.password());
        }
        if (demoSettings.enabled() && !demoSettings.canSeed()) {
            log.warn("DEMO_MODE ativo, mas DEMO_PASSWORD não foi definida: dados de demonstração não criados");
        }
        demoSeeder.seed(demoSettings);
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "America/Sao_Paulo")
    void purgeExpiredTokens() {
        int removed = tokenCleaner.purge();
        log.info("Limpeza de tokens expirados: {} removidos", removed);
    }
}
