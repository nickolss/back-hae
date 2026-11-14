package br.com.cps.hae.application.auth;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.PasswordHasher;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.shared.EmailAddress;
import br.com.cps.hae.domain.shared.PasswordPolicy;
import jakarta.transaction.Transactional;

/** Cria o primeiro administrador quando ainda não existe nenhum (sem usuário padrão embutido no código). */
@UseCase
@Transactional
public class InitialAdminProvisioner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminProvisioner.class);

    private final EmployeeRepository employees;
    private final PasswordHasher hasher;
    private final Clock clock;

    public InitialAdminProvisioner(EmployeeRepository employees, PasswordHasher hasher, Clock clock) {
        this.employees = employees;
        this.hasher = hasher;
        this.clock = clock;
    }

    public void provision(String name, String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (employees.existsByRole(Role.ADMIN)) {
            return;
        }
        EmailAddress address = new EmailAddress(email);
        if (employees.existsByEmail(address)) {
            log.warn("Admin inicial não criado: e-mail já usado por outra conta");
            return;
        }
        PasswordPolicy.validate(password);
        employees.save(Employee.create(name == null || name.isBlank() ? "Administrador" : name, address,
                hasher.hash(password), Role.ADMIN, null, null, clock.instant()));
        log.info("Administrador inicial criado");
    }
}
