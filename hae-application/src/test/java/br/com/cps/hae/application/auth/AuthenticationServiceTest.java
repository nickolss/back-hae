package br.com.cps.hae.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.cps.hae.application.account.ProfileAssembler;
import br.com.cps.hae.application.port.CourseRepository;
import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.port.Notifier;
import br.com.cps.hae.application.port.PasswordHasher;
import br.com.cps.hae.application.port.PendingRegistrationRepository;
import br.com.cps.hae.application.port.SecureTokens;
import br.com.cps.hae.application.port.SessionTokenIssuer;
import br.com.cps.hae.application.shared.AuthenticationFailedException;
import br.com.cps.hae.domain.auth.PendingRegistration;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.institution.Course;
import br.com.cps.hae.domain.institution.Institution;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.EmailAddress;

class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final InstitutionRepository institutions = mock(InstitutionRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final PendingRegistrationRepository pending = mock(PendingRegistrationRepository.class);
    private final SessionTokenIssuer sessions = mock(SessionTokenIssuer.class);
    private final Notifier notifier = mock(Notifier.class);
    private final PasswordHasher hasher = new PasswordHasher() {
        @Override
        public String hash(String raw) {
            return "h:" + raw;
        }

        @Override
        public boolean matches(String raw, String hash) {
            return hash.equals("h:" + raw);
        }
    };
    private final SecureTokens tokens = new SecureTokens() {
        @Override
        public String generate() {
            return "token-123";
        }

        @Override
        public String hash(String token) {
            return "sha:" + token;
        }
    };

    private ProfileAssembler profiles;
    private Institution fatec;
    private Course course;

    @BeforeEach
    void setUp() {
        profiles = new ProfileAssembler(institutions, courses);
        fatec = Institution.create(110, "Fatec ZL", "Rua", 100, NOW);
        course = Course.create(fatec.id(), "ADS");
        when(institutions.findByCode(110)).thenReturn(Optional.of(fatec));
        when(institutions.findById(fatec.id())).thenReturn(Optional.of(fatec));
        when(courses.findById(course.id())).thenReturn(Optional.of(course));
        when(sessions.issue(any())).thenReturn(new SessionTokenIssuer.SessionToken("jwt", NOW.plusSeconds(60)));
        when(employees.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void loginComSenhaErradaFalhaComMensagemGenerica() {
        Employee e = Employee.create("Ana", new EmailAddress("ana@fatec.sp.gov.br"), "h:segredo123",
                Role.PROFESSOR, fatec.id(), null, NOW);
        when(employees.findByEmail(e.email())).thenReturn(Optional.of(e));
        var service = new AuthenticationService(employees, hasher, sessions, profiles);

        assertThat(service.login("ana@fatec.sp.gov.br", "segredo123").token().value()).isEqualTo("jwt");
        assertThatThrownBy(() -> service.login("ana@fatec.sp.gov.br", "errada"))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("E-mail ou senha inválidos.");
        assertThatThrownBy(() -> service.login("naoexiste@fatec.sp.gov.br", "x"))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("E-mail ou senha inválidos.");
    }

    @Test
    void cadastroGuardaApenasHashesEEnviaTokenPorEmail() {
        var service = registration(new RegistrationSettings("", List.of()));
        service.register(new RegistrationService.RegisterCommand("Ana", "ana@fatec.sp.gov.br", "senhaForte1", 110,
                course.id()));
        verify(pending).save(org.mockito.ArgumentMatchers.argThat(p -> p.tokenHash().equals("sha:token-123")
                && p.passwordHash().equals("h:senhaForte1")));
        verify(notifier).sendAccountActivation(eq(new EmailAddress("ana@fatec.sp.gov.br")), eq("Ana"),
                eq("token-123"));
    }

    @Test
    void cadastroDeEmailExistenteNaoRevelaExistencia() {
        when(employees.existsByEmail(any())).thenReturn(true);
        registration(new RegistrationSettings("", List.of())).register(
                new RegistrationService.RegisterCommand("Ana", "ana@fatec.sp.gov.br", "senhaForte1", 110,
                        course.id()));
        verify(notifier, never()).sendAccountActivation(any(), anyString(), anyString());
    }

    @Test
    void dominioNaoPermitidoERejeitado() {
        var service = registration(new RegistrationSettings("", List.of("fatec.sp.gov.br")));
        assertThatThrownBy(() -> service.register(new RegistrationService.RegisterCommand("Ana", "ana@gmail.com",
                "senhaForte1", 110, course.id()))).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void ativacaoCriaProfessorEInicia_sessao() {
        var reg = PendingRegistration.create("Ana", new EmailAddress("ana@cps.sp.gov.br"), "h:x", fatec.id(),
                course.id(), "sha:token-123", NOW);
        when(pending.findByTokenHash("sha:token-123")).thenReturn(Optional.of(reg));

        var session = registration(new RegistrationSettings("", List.of())).activate("token-123");

        assertThat(session.profile().role()).isEqualTo(Role.PROFESSOR);
        assertThat(session.profile().institution().code()).isEqualTo(110);
        verify(pending).delete(reg.id());
    }

    @Test
    void ativacaoComTokenExpiradoFalha() {
        var reg = new PendingRegistration(UUID.randomUUID(), "Ana", new EmailAddress("ana@x.com"), "h", fatec.id(),
                course.id(), "sha:token-123", NOW.minusSeconds(1));
        when(pending.findByTokenHash("sha:token-123")).thenReturn(Optional.of(reg));
        assertThatThrownBy(() -> registration(new RegistrationSettings("", List.of())).activate("token-123"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    private RegistrationService registration(RegistrationSettings settings) {
        return new RegistrationService(employees, institutions, courses, pending, hasher, tokens, sessions,
                notifier, profiles, settings, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
