package br.com.cps.hae.web.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.cps.hae.application.account.EmployeeProfile;
import br.com.cps.hae.application.employee.EmployeeManagementService;
import br.com.cps.hae.application.employee.EmployeeManagementService.CreateEmployeeCommand;
import br.com.cps.hae.application.employee.EmployeeManagementService.UpdateEmployeeCommand;
import br.com.cps.hae.application.port.EmployeeQueries.EmployeeSummary;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.PageQuery;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.web.security.CurrentActor;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Funcionários")
class EmployeeController {

    private final EmployeeManagementService employees;

    EmployeeController(EmployeeManagementService employees) {
        this.employees = employees;
    }

    record CreateEmployeeRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 100) String name,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
            @NotNull(message = "Selecione o papel.") Role role,
            UUID institutionId,
            UUID courseId,
            @NotBlank @Size(min = 8, max = 72, message = "A senha provisória deve ter entre 8 e 72 caracteres.") String temporaryPassword) {
    }

    record UpdateEmployeeRequest(Role role, UUID courseId) {
    }

    @GetMapping
    PageResponse<EmployeeSummary> search(@Parameter(hidden = true) @CurrentActor Actor actor,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(employees.search(actor, institutionId, role, q, new PageQuery(page, size)));
    }

    @GetMapping("/{id}")
    EmployeeProfile get(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id) {
        return employees.get(actor, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EmployeeProfile create(@Parameter(hidden = true) @CurrentActor Actor actor,
            @Valid @RequestBody CreateEmployeeRequest request) {
        return employees.create(actor, new CreateEmployeeCommand(request.name(), request.email(), request.role(),
                request.institutionId(), request.courseId(), request.temporaryPassword()));
    }

    @PatchMapping("/{id}")
    EmployeeProfile update(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return employees.update(actor, id, new UpdateEmployeeCommand(request.role(), request.courseId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id) {
        employees.delete(actor, id);
    }
}
