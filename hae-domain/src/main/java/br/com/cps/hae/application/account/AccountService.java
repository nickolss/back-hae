package br.com.cps.hae.application.account;

import java.util.Optional;
import java.util.UUID;

import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.Notifier;
import br.com.cps.hae.application.port.PasswordHasher;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.AuthenticationFailedException;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.shared.NotFoundException;
import br.com.cps.hae.domain.shared.PasswordPolicy;
import jakarta.transaction.Transactional;

/** Casos de uso da própria conta do usuário autenticado. */
@UseCase
@Transactional
public class AccountService {

    private final EmployeeRepository employees;
    private final PasswordHasher hasher;
    private final ProfileAssembler profiles;
    private final Notifier notifier;

    public AccountService(EmployeeRepository employees, PasswordHasher hasher, ProfileAssembler profiles,
            Notifier notifier) {
        this.employees = employees;
        this.hasher = hasher;
        this.profiles = profiles;
        this.notifier = notifier;
    }

    /** Resolve o usuário de uma sessão; vazio se a conta não existe mais. */
    public Optional<Actor> loadActor(UUID employeeId) {
        return employees.findById(employeeId).map(Actor::of);
    }

    public EmployeeProfile me(Actor actor) {
        return profiles.toProfile(load(actor));
    }

    public EmployeeProfile updateProfile(Actor actor, String name) {
        Employee employee = load(actor);
        employee.rename(name);
        return profiles.toProfile(employees.save(employee));
    }

    public void changePassword(Actor actor, String currentPassword, String newPassword) {
        Employee employee = load(actor);
        if (!hasher.matches(currentPassword, employee.passwordHash())) {
            throw new AuthenticationFailedException("Senha atual incorreta.");
        }
        PasswordPolicy.validate(newPassword);
        employee.changePassword(hasher.hash(newPassword));
        employees.save(employee);
        notifier.sendPasswordChanged(employee.email(), employee.name());
    }

    private Employee load(Actor actor) {
        return employees.findById(actor.id()).orElseThrow(() -> NotFoundException.of("Usuário"));
    }
}
