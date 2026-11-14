package br.com.cps.hae.web.api;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.cps.hae.application.account.AccountService;
import br.com.cps.hae.application.account.EmployeeProfile;
import br.com.cps.hae.application.hae.HaeQueryService;
import br.com.cps.hae.application.port.HaeQueries;
import br.com.cps.hae.application.port.HaeQueries.HaeSummary;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.PageQuery;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.shared.Semester;
import br.com.cps.hae.web.security.CurrentActor;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Minha conta")
class MeController {

    private final AccountService accounts;
    private final HaeQueryService haes;

    MeController(AccountService accounts, HaeQueryService haes) {
        this.accounts = accounts;
        this.haes = haes;
    }

    record UpdateProfileRequest(@NotBlank(message = "Informe seu nome.") @Size(max = 100) String name) {
    }

    record ChangePasswordRequest(@NotBlank(message = "Informe a senha atual.") String currentPassword,
            @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.") String newPassword) {
    }

    @GetMapping
    EmployeeProfile me(@Parameter(hidden = true) @CurrentActor Actor actor) {
        return accounts.me(actor);
    }

    @PatchMapping
    EmployeeProfile update(@Parameter(hidden = true) @CurrentActor Actor actor,
            @Valid @RequestBody UpdateProfileRequest request) {
        return accounts.updateProfile(actor, request.name());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Parameter(hidden = true) @CurrentActor Actor actor,
            @Valid @RequestBody ChangePasswordRequest request) {
        accounts.changePassword(actor, request.currentPassword(), request.newPassword());
    }

    @GetMapping("/haes")
    PageResponse<HaeSummary> myHaes(@Parameter(hidden = true) @CurrentActor Actor actor,
            @RequestParam(required = false) HaeStatus status,
            @RequestParam(required = false) HaeType type,
            @RequestParam(required = false) Semester semester,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var criteria = new HaeQueries.Criteria(null, null, null, type, status, null, semester, q);
        return PageResponse.of(haes.mine(actor, criteria, new PageQuery(page, size)));
    }

}
