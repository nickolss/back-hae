package br.com.cps.hae;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;

import br.com.cps.hae.application.port.FileStorage;
import br.com.cps.hae.application.port.Notifier;
import jakarta.servlet.http.Cookie;

/**
 * Fluxo ponta a ponta contra um PostgreSQL real (Flyway + JPA + segurança + casos de uso).
 */
@Testcontainers
@SpringBootTest(properties = {
        "hae.jwt.secret=integration-test-secret-0123456789abcdef",
        "hae.admin.email=admin@hae.test",
        "hae.admin.password=admin-pass-123",
        "hae.web.cookie-name=hae_session",
        "hae.web.cookie-secure=false",
        "hae.web.rate-limit.auth-per-minute=1000",
        "logging.structured.format.console=" })
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    Notifier notifier;

    @MockitoBean
    FileStorage storage;

    /** Navegador simulado: guarda cookies (sessão + XSRF) por usuário. */
    final class Browser {
        final Map<String, Cookie> cookies = new HashMap<>();

        MvcResult perform(MockHttpServletRequestBuilder request) throws Exception {
            if (!cookies.isEmpty()) {
                request.cookie(cookies.values().toArray(Cookie[]::new));
            }
            Cookie xsrf = cookies.get("XSRF-TOKEN");
            if (xsrf != null) {
                request.header("X-XSRF-TOKEN", xsrf.getValue());
            }
            MvcResult result = mvc.perform(request).andReturn();
            for (Cookie c : result.getResponse().getCookies()) {
                if (c.getMaxAge() == 0) {
                    cookies.remove(c.getName());
                } else {
                    cookies.put(c.getName(), c);
                }
            }
            return result;
        }

        MvcResult json(MockHttpServletRequestBuilder request, String body) throws Exception {
            return perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
        }

        Browser login(String email, String password) throws Exception {
            perform(get("/api/v1/auth/csrf"));
            MvcResult r = json(post("/api/v1/auth/login"),
                    "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
            assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
            return this;
        }
    }

    Browser admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = new Browser().login("admin@hae.test", "admin-pass-123");
    }

    @Test
    void rotasProtegidasExigemSessao() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/api/v1/haes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void postSemTokenCsrfEBloqueado() throws Exception {
        var browser = new Browser();
        MvcResult r = browser.json(post("/api/v1/institutions").cookie(admin.cookies.get("hae_session")), "{}");
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void loginInvalidoNaoRevelaSeEmailExiste() throws Exception {
        var browser = new Browser();
        browser.perform(get("/api/v1/auth/csrf"));
        String a = browser.json(post("/api/v1/auth/login"), "{\"email\":\"admin@hae.test\",\"password\":\"x\"}")
                .getResponse().getContentAsString();
        String b = browser.json(post("/api/v1/auth/login"), "{\"email\":\"ninguem@hae.test\",\"password\":\"x\"}")
                .getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(a, "$.detail")).isEqualTo(JsonPath.read(b, "$.detail"));
    }

    @Test
    void respostasNaoExpoemHashDeSenha() throws Exception {
        String me = admin.perform(get("/api/v1/me")).getResponse().getContentAsString();
        assertThat(me).doesNotContain("password").contains("ADMIN");
    }

    @Test
    void cicloCompletoDeUmaHae() throws Exception {
        // admin cria unidade e curso
        String inst = admin.json(post("/api/v1/institutions"),
                "{\"code\":%d,\"name\":\"Fatec %d\",\"address\":\"Rua A, 1\",\"weeklyHoursLimit\":20}"
                        .formatted(code(), code()))
                .getResponse().getContentAsString();
        String institutionId = JsonPath.read(inst, "$.id");
        String course = admin.json(post("/api/v1/institutions/" + institutionId + "/courses"), "{\"name\":\"ADS\"}")
                .getResponse().getContentAsString();
        String courseId = JsonPath.read(course, "$.id");

        // admin cadastra professor e coordenador
        String suffix = String.valueOf(System.nanoTime());
        createEmployee(institutionId, courseId, "prof" + suffix + "@fatec.test", "PROFESSOR");
        createEmployee(institutionId, courseId, "coord" + suffix + "@fatec.test", "COORDENADOR");
        Browser professor = new Browser().login("prof" + suffix + "@fatec.test", "senha-temp-123");
        Browser coordinator = new Browser().login("coord" + suffix + "@fatec.test", "senha-temp-123");

        // professor não pode listar funcionários
        assertThat(professor.perform(get("/api/v1/employees")).getResponse().getStatus()).isEqualTo(403);

        // professor cria HAE
        MvcResult created = professor.json(post("/api/v1/haes"), haeJson(courseId, 6));
        assertThat(created.getResponse().getStatus()).as(created.getResponse().getContentAsString()).isEqualTo(201);
        String haeId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        assertThat(JsonPath.<Boolean>read(created.getResponse().getContentAsString(), "$.permissions.canEdit")).isTrue();

        // limite de horas da unidade (20h) é respeitado
        MvcResult tooMuch = professor.json(post("/api/v1/haes"), haeJson(courseId, 15));
        assertThat(tooMuch.getResponse().getStatus()).isEqualTo(422);

        // coordenador vê na listagem da instituição e aprova
        String list = coordinator.perform(get("/api/v1/haes?status=PENDENTE")).getResponse().getContentAsString();
        assertThat(JsonPath.<Integer>read(list, "$.totalElements")).isEqualTo(1);
        MvcResult approved = coordinator.json(post("/api/v1/haes/" + haeId + "/review"),
                "{\"decision\":\"APPROVE\",\"comment\":\"Ok\"}");
        assertThat(JsonPath.<String>read(approved.getResponse().getContentAsString(), "$.status"))
                .isEqualTo("APROVADO");

        // professor envia fechamento; coordenador aprova => COMPLETO
        MvcResult closure = professor.json(post("/api/v1/haes/" + haeId + "/closure"),
                "{\"type\":\"TCC\",\"role\":\"Orientador\",\"studentCount\":2,\"studentNames\":\"Ana; Bia\"}");
        assertThat(closure.getResponse().getStatus()).as(closure.getResponse().getContentAsString()).isEqualTo(200);
        MvcResult done = coordinator.json(post("/api/v1/haes/" + haeId + "/closure/review"),
                "{\"decision\":\"APPROVE\"}");
        String doneBody = done.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(doneBody, "$.status")).isEqualTo("COMPLETO");
        assertThat(JsonPath.<String>read(doneBody, "$.closures[0].studentNames")).isEqualTo("Ana; Bia");

        // HAE completa não pode ser excluída
        assertThat(professor.perform(delete("/api/v1/haes/" + haeId)).getResponse().getStatus()).isEqualTo(422);

        // dashboard do diretor/admin
        String dash = admin.perform(get("/api/v1/dashboard?institutionId=" + institutionId + "&semester=2026-2"))
                .getResponse().getContentAsString();
        assertThat(JsonPath.<Integer>read(dash, "$.stats.byStatus.COMPLETO")).isEqualTo(1);
    }

    @Test
    void professorNaoAlteraPapelDeNinguem() throws Exception {
        String inst = admin.json(post("/api/v1/institutions"),
                "{\"code\":%d,\"name\":\"Fatec X%d\",\"address\":\"Rua B\",\"weeklyHoursLimit\":10}"
                        .formatted(code(), code()))
                .getResponse().getContentAsString();
        String institutionId = JsonPath.read(inst, "$.id");
        String suffix = String.valueOf(System.nanoTime());
        String profId = createEmployee(institutionId, null, "p" + suffix + "@fatec.test", "PROFESSOR");
        Browser professor = new Browser().login("p" + suffix + "@fatec.test", "senha-temp-123");

        MvcResult r = professor.json(patch("/api/v1/employees/" + profId), "{\"role\":\"ADMIN\"}");
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
    }

    private String createEmployee(String institutionId, String courseId, String email, String role)
            throws Exception {
        MvcResult r = admin.json(post("/api/v1/employees"), """
                {"name":"Pessoa Teste","email":"%s","role":"%s","institutionId":"%s",%s"temporaryPassword":"senha-temp-123"}"""
                .formatted(email, role, institutionId, courseId == null ? "" : "\"courseId\":\"" + courseId + "\","));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private static String haeJson(String courseId, int hours) {
        return """
                {"courseId":"%s","title":"Projeto de TCC","description":"Orientação de TCC","type":"TCC",
                 "modality":"PRESENCIAL","dimension":"DIMENSAO_1_DIDATICO_PEDAGOGICO","weeklyHours":%d,
                 "startDate":"2026-08-03","endDate":"2026-12-04",
                 "schedule":[{"day":"MONDAY","start":"08:00","end":"10:00"}],
                 "studentRas":["1110482312345"]}""".formatted(courseId, hours);
    }

    private static int code() {
        return (int) (System.nanoTime() % 900_000) + 1000;
    }
}
