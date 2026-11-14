package br.com.cps.hae.application.auth;

import java.time.Clock;

import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.Notifier;
import br.com.cps.hae.application.port.PasswordHasher;
import br.com.cps.hae.application.port.PasswordResetTokenRepository;
import br.com.cps.hae.application.port.SecureTokens;
import br.com.cps.hae.application.shared.AuthenticationFailedException;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.auth.PasswordResetToken;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.EmailAddress;
import br.com.cps.hae.domain.shared.Guard;
import br.com.cps.hae.domain.shared.NotFoundException;
import br.com.cps.hae.domain.shared.PasswordPolicy;
import jakarta.transaction.Transactional;

@UseCase
@Transactional
public class PasswordRecoveryService {

    private static final String INVALID_TOKEN = "Link de redefinição inválido ou expirado.";

    private final EmployeeRepository employees;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordHasher hasher;
    private final SecureTokens tokens;
    private final Notifier notifier;
    private final Clock clock;

    public PasswordRecoveryService(EmployeeRepository employees, PasswordResetTokenRepository resetTokens,
            PasswordHasher hasher, SecureTokens tokens, Notifier notifier, Clock clock) {
        this.employees = employees;
        this.resetTokens = resetTokens;
        this.hasher = hasher;
        this.tokens = tokens;
        this.notifier = notifier;
        this.clock = clock;
    }

    /** Sempre "sucesso" para o chamador, exista ou não a conta (evita enumeração). */
    public void requestReset(String email) {
        EmailAddress address;
        try {
            address = new EmailAddress(email);
        } catch (BusinessRuleViolationException e) {
            return;
        }
        employees.findByEmail(address).ifPresent(employee -> {
            resetTokens.deleteByEmployee(employee.id());
            String token = tokens.generate();
            resetTokens.save(PasswordResetToken.create(employee.id(), tokens.hash(token), clock.instant()));
            notifier.sendPasswordReset(employee.email(), employee.name(), token);
        });
    }

    public void reset(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokens.findByTokenHash(tokens.hash(Guard.notNull(token, "token")))
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_TOKEN));
        if (resetToken.isExpired(clock.instant())) {
            resetTokens.deleteByEmployee(resetToken.employeeId());
            throw new AuthenticationFailedException(INVALID_TOKEN);
        }
        PasswordPolicy.validate(newPassword);
        Employee employee = employees.findById(resetToken.employeeId())
                .orElseThrow(() -> NotFoundException.of("Usuário"));
        employee.changePassword(hasher.hash(newPassword));
        employees.save(employee);
        resetTokens.deleteByEmployee(employee.id());
        notifier.sendPasswordChanged(employee.email(), employee.name());
    }
}
