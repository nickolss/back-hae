package br.com.cps.hae.application.employee;

import java.time.Clock;
import java.util.UUID;

import br.com.cps.hae.application.account.EmployeeProfile;
import br.com.cps.hae.application.account.ProfileAssembler;
import br.com.cps.hae.application.port.CourseRepository;
import br.com.cps.hae.application.port.EmployeeQueries;
import br.com.cps.hae.application.port.EmployeeQueries.EmployeeSummary;
import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.HaeRepository;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.port.PasswordHasher;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.PageQuery;
import br.com.cps.hae.application.shared.PageResult;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.shared.AccessDeniedException;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.ConflictException;
import br.com.cps.hae.domain.shared.EmailAddress;
import br.com.cps.hae.domain.shared.NotFoundException;
import br.com.cps.hae.domain.shared.PasswordPolicy;
import jakarta.transaction.Transactional;

/** Gestão de funcionários por diretores e administradores. */
@UseCase
@Transactional
public class EmployeeManagementService {

    private final EmployeeRepository employees;
    private final EmployeeQueries queries;
    private final InstitutionRepository institutions;
    private final CourseRepository courses;
    private final HaeRepository haes;
    private final PasswordHasher hasher;
    private final ProfileAssembler profiles;
    private final Clock clock;

    public EmployeeManagementService(EmployeeRepository employees, EmployeeQueries queries,
            InstitutionRepository institutions, CourseRepository courses, HaeRepository haes, PasswordHasher hasher,
            ProfileAssembler profiles, Clock clock) {
        this.employees = employees;
        this.queries = queries;
        this.institutions = institutions;
        this.courses = courses;
        this.haes = haes;
        this.hasher = hasher;
        this.profiles = profiles;
        this.clock = clock;
    }

    public record CreateEmployeeCommand(String name, String email, Role role, UUID institutionId, UUID courseId,
            String temporaryPassword) {
    }

    public record UpdateEmployeeCommand(Role role, UUID courseId) {
    }

    public PageResult<EmployeeSummary> search(Actor actor, UUID institutionId, Role role, String text,
            PageQuery page) {
        if (!actor.role().canViewInstitutionHaes()) {
            throw new AccessDeniedException();
        }
        var criteria = new EmployeeQueries.Criteria(actor.scopeInstitution(institutionId), role, text);
        return queries.search(criteria, page);
    }

    public EmployeeProfile get(Actor actor, UUID id) {
        Employee employee = loadVisible(actor, id);
        return profiles.toProfile(employee);
    }

    public EmployeeProfile create(Actor actor, CreateEmployeeCommand command) {
        UUID institutionId = command.role() != null && command.role().isGlobal() ? null
                : actor.scopeInstitution(command.institutionId());
        if (institutionId != null) {
            actor.requireInstitutionManager(institutionId);
            institutions.findById(institutionId).orElseThrow(() -> NotFoundException.of("Instituição"));
        }
        ensureCanAssign(actor, command.role());
        EmailAddress email = new EmailAddress(command.email());
        if (employees.existsByEmail(email)) {
            throw new ConflictException("Já existe uma conta com este e-mail.");
        }
        validateCourse(command.courseId(), institutionId);
        PasswordPolicy.validate(command.temporaryPassword());
        Employee employee = Employee.create(command.name(), email, hasher.hash(command.temporaryPassword()),
                command.role(), institutionId, command.courseId(), clock.instant());
        return profiles.toProfile(employees.save(employee));
    }

    public EmployeeProfile update(Actor actor, UUID id, UpdateEmployeeCommand command) {
        Employee employee = loadVisible(actor, id);
        requireManagerOf(actor, employee);
        if (employee.id().equals(actor.id())) {
            throw new BusinessRuleViolationException("Você não pode alterar o próprio papel.");
        }
        if (command.role() != null && command.role() != employee.role()) {
            ensureCanAssign(actor, employee.role());
            ensureCanAssign(actor, command.role());
            employee.changeRole(command.role());
        }
        if (command.courseId() != null) {
            validateCourse(command.courseId(), employee.institutionId());
            employee.assignCourse(command.courseId());
        }
        return profiles.toProfile(employees.save(employee));
    }

    public void delete(Actor actor, UUID id) {
        Employee employee = loadVisible(actor, id);
        requireManagerOf(actor, employee);
        if (employee.id().equals(actor.id())) {
            throw new BusinessRuleViolationException("Você não pode excluir a própria conta por aqui.");
        }
        ensureCanAssign(actor, employee.role());
        if (haes.existsByEmployee(employee.id())) {
            throw new ConflictException("O funcionário possui HAEs registradas e não pode ser excluído.");
        }
        employees.delete(employee.id());
    }

    private Employee loadVisible(Actor actor, UUID id) {
        if (!actor.role().canViewInstitutionHaes() && !actor.id().equals(id)) {
            throw new AccessDeniedException();
        }
        Employee employee = employees.findById(id).orElseThrow(() -> NotFoundException.of("Funcionário"));
        if (!actor.isGlobal() && !employee.belongsTo(actor.institutionId())) {
            throw NotFoundException.of("Funcionário");
        }
        return employee;
    }

    private static void requireManagerOf(Actor actor, Employee employee) {
        if (employee.institutionId() != null) {
            actor.requireInstitutionManager(employee.institutionId());
        } else {
            actor.requireGlobal();
        }
    }

    private void ensureCanAssign(Actor actor, Role role) {
        if (role == null) {
            throw new BusinessRuleViolationException("O papel é obrigatório.");
        }
        if (!actor.role().canAssign(role)) {
            throw new AccessDeniedException("Você não pode gerenciar usuários com o papel " + role + ".");
        }
    }

    private void validateCourse(UUID courseId, UUID institutionId) {
        if (courseId == null) {
            return;
        }
        courses.findById(courseId)
                .filter(c -> c.institutionId().equals(institutionId))
                .orElseThrow(() -> NotFoundException.of("Curso"));
    }
}
