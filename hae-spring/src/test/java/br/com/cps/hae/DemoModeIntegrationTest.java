package br.com.cps.hae;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.security.SecureRandom;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;

import br.com.cps.hae.application.demo.DemoDataSeeder;
import br.com.cps.hae.application.demo.DemoSettings;
import br.com.cps.hae.application.port.Notifier;
import jakarta.servlet.http.Cookie;

/** Modo demonstração: dados fictícios coerentes, idempotentes e sem envio de e-mails. */
@Testcontainers
@SpringBootTest(properties = { "hae.demo.enabled=true", "hae.web.cookie-name=hae_session",
        "hae.web.cookie-secure=false", "hae.web.rate-limit.auth-per-minute=1000", "logging.structured.format.console=" })
@AutoConfigureMockMvc
class DemoModeIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final SecureRandom RANDOM = new SecureRandom();
    static final String DEMO_PASSWORD = random();

    @DynamicPropertySource
    static void secrets(DynamicPropertyRegistry registry) {
        registry.add("hae.jwt.secret", DemoModeIntegrationTest::random);
        registry.add("hae.demo.password", () -> DEMO_PASSWORD);
    }

    static String random() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoDataSeeder seeder;

    @MockitoBean
    Notifier notifier;

    @Test
    void infoPublicaExpoeContasSemSenha() throws Exception {
        String info = mvc.perform(get("/api/v1/public/info")).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<Boolean>read(info, "$.demo")).isTrue();
        assertThat(JsonPath.<Integer>read(info, "$.demoAccounts.length()")).isEqualTo(7);
        assertThat(info).doesNotContain(DEMO_PASSWORD).doesNotContainIgnoringCase("password");
    }

    @Test
    void diretoraEntraEVeTodosOsEstadosDaHae() throws Exception {
        MvcResult csrf = mvc.perform(get("/api/v1/auth/csrf")).andReturn();
        Cookie xsrf = csrf.getResponse().getCookie("XSRF-TOKEN");
        MvcResult login = mvc.perform(post("/api/v1/auth/login").cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"diretora@fatec-demo.example.com\",\"password\":\"" + DEMO_PASSWORD + "\"}"))
                .andReturn();
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        Cookie session = login.getResponse().getCookie("hae_session");

        String dash = mvc.perform(get("/api/v1/dashboard").cookie(session)).andReturn().getResponse()
                .getContentAsString();
        assertThat(JsonPath.<Integer>read(dash, "$.stats.totalHaes")).isEqualTo(8);
        for (String status : new String[] { "PENDENTE", "APROVADO", "REPROVADO", "FECHAMENTO_SOLICITADO",
                "COMPLETO" }) {
            assertThat(JsonPath.<Integer>read(dash, "$.stats.byStatus." + status)).as(status).isPositive();
        }
    }

    @Test
    void naoDuplicaAoReiniciar() {
        assertThat(seeder.seed(new DemoSettings(true, DEMO_PASSWORD))).isFalse();
    }
}
