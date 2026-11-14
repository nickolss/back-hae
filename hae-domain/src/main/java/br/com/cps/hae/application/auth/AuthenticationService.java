package br.com.cps.hae.application.auth;

import br.com.cps.hae.application.account.ProfileAssembler;
import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.PasswordHasher;
import br.com.cps.hae.application.port.SessionTokenIssuer;
import br.com.cps.hae.application.shared.AuthenticationFailedException;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.EmailAddress;
import jakarta.transaction.Transactional;

@UseCase
@Transactional
public class AuthenticationService {

    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos.";

    private final EmployeeRepository employees;
    private final PasswordHasher hasher;
    private final SessionTokenIssuer sessions;
    private final ProfileAssembler profiles;
    /** Hash fictício usado para igualar o tempo de resposta quando o e-mail não existe. */
    private final String dummyHash;

    public AuthenticationService(EmployeeRepository employees, PasswordHasher hasher, SessionTokenIssuer sessions,
            ProfileAssembler profiles) {
        this.employees = employees;
        this.hasher = hasher;
        this.sessions = sessions;
        this.profiles = profiles;
        this.dummyHash = hasher.hash("timing-equalizer-not-a-password");
    }

    public AuthenticatedSession login(String email, String password) {
        EmailAddress address;
        try {
            address = new EmailAddress(email);
        } catch (BusinessRuleViolationException e) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        String raw = password == null ? "" : password;
        Employee employee = employees.findByEmail(address).orElse(null);
        if (employee == null) {
            hasher.matches(raw, dummyHash);
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        if (!hasher.matches(raw, employee.passwordHash())) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        return new AuthenticatedSession(profiles.toProfile(employee), sessions.issue(employee));
    }
}
