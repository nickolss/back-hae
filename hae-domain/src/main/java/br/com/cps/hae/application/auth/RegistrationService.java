package br.com.cps.hae.application.auth;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.auth.PendingRegistration;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.institution.Course;
import br.com.cps.hae.domain.institution.Institution;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.ConflictException;
import br.com.cps.hae.domain.shared.EmailAddress;
import br.com.cps.hae.domain.shared.Guard;
import br.com.cps.hae.domain.shared.NotFoundException;
import br.com.cps.hae.domain.shared.PasswordPolicy;
import jakarta.transaction.Transactional;

/** Autocadastro com confirmação por e-mail. */
@UseCase
@Transactional
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final EmployeeRepository employees;
    private final InstitutionRepository institutions;
    private final CourseRepository courses;
    private final PendingRegistrationRepository pending;
    private final PasswordHasher hasher;
    private final SecureTokens tokens;
    private final SessionTokenIssuer sessions;
    private final Notifier notifier;
    private final ProfileAssembler profiles;
    private final RegistrationSettings settings;
    private final Clock clock;

    public RegistrationService(EmployeeRepository employees, InstitutionRepository institutions,
            CourseRepository courses, PendingRegistrationRepository pending, PasswordHasher hasher,
            SecureTokens tokens, SessionTokenIssuer sessions, Notifier notifier, ProfileAssembler profiles,
            RegistrationSettings settings, Clock clock) {
        this.employees = employees;
        this.institutions = institutions;
        this.courses = courses;
        this.pending = pending;
        this.hasher = hasher;
        this.tokens = tokens;
        this.sessions = sessions;
        this.notifier = notifier;
        this.profiles = profiles;
        this.settings = settings;
        this.clock = clock;
    }

    public record RegisterCommand(String name, String email, String password, int institutionCode, UUID courseId) {
    }

    /**
     * Inicia o cadastro. A resposta é sempre a mesma, exista ou não a conta, para evitar enumeração de e-mails.
     */
    public void register(RegisterCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        String name = Guard.notBlank(command.name(), "nome", Employee.NAME_MAX_LENGTH);
        PasswordPolicy.validate(command.password());
        ensureAllowedDomain(email);

        Institution institution = institutions.findByCode(command.institutionCode())
                .filter(Institution::active)
                .orElseThrow(() -> NotFoundException.of("Instituição"));
        Course course = courses.findById(Guard.notNull(command.courseId(), "curso"))
                .filter(c -> c.institutionId().equals(institution.id()))
                .orElseThrow(() -> NotFoundException.of("Curso"));

        if (employees.existsByEmail(email)) {
            log.info("Cadastro ignorado: e-mail já registrado");
            return;
        }

        pending.deleteByEmail(email);
        String token = tokens.generate();
        pending.save(PendingRegistration.create(name, email, hasher.hash(command.password()), institution.id(),
                course.id(), tokens.hash(token), clock.instant()));
        notifier.sendAccountActivation(email, name, token);
    }

    public AuthenticatedSession activate(String token) {
        PendingRegistration registration = pending.findByTokenHash(tokens.hash(Guard.notNull(token, "token")))
                .orElseThrow(() -> new AuthenticationFailedException("Link de ativação inválido ou expirado."));
        if (registration.isExpired(clock.instant())) {
            pending.delete(registration.id());
            throw new AuthenticationFailedException("Link de ativação inválido ou expirado.");
        }
        if (employees.existsByEmail(registration.email())) {
            pending.delete(registration.id());
            throw new ConflictException("Este e-mail já possui uma conta ativa.");
        }

        Employee employee = employees.save(Employee.create(registration.name(), registration.email(),
                registration.passwordHash(), Role.PROFESSOR, registration.institutionId(),
                registration.courseId(), clock.instant()));
        pending.delete(registration.id());
        return new AuthenticatedSession(profiles.toProfile(employee), sessions.issue(employee));
    }

    private void ensureAllowedDomain(EmailAddress email) {
        if (!settings.allowedEmailDomains().isEmpty()
                && settings.allowedEmailDomains().stream().noneMatch(email::hasDomain)) {
            throw new BusinessRuleViolationException(
                    "Use seu e-mail institucional (" + String.join(", ", settings.allowedEmailDomains()) + ").");
        }
    }
}
