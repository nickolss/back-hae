package br.com.cps.hae.web.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.cps.hae.application.institution.CourseService;
import br.com.cps.hae.application.institution.CourseService.CourseView;
import br.com.cps.hae.application.institution.HoursSummary;
import br.com.cps.hae.application.institution.InstitutionService;
import br.com.cps.hae.application.institution.InstitutionService.InstitutionCommand;
import br.com.cps.hae.application.institution.InstitutionView;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.domain.shared.Semester;
import br.com.cps.hae.web.security.CurrentActor;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/institutions")
@Tag(name = "Instituições e cursos")
class InstitutionController {

    private final InstitutionService institutions;
    private final CourseService courses;

    InstitutionController(InstitutionService institutions, CourseService courses) {
        this.institutions = institutions;
        this.courses = courses;
    }

    record InstitutionRequest(
            @NotNull(message = "Informe o código.") @Positive(message = "O código deve ser positivo.") Integer code,
            @NotBlank(message = "Informe o nome.") @Size(max = 150) String name,
            @NotBlank(message = "Informe o endereço.") @Size(max = 255) String address,
            @NotNull(message = "Informe o limite de horas.") @Min(0) @Max(10_000) Integer weeklyHoursLimit) {

        InstitutionCommand toCommand() {
            return new InstitutionCommand(code, name, address, weeklyHoursLimit);
        }
    }

    record HoursLimitRequest(@NotNull @Min(0) @Max(10_000) Integer weeklyHoursLimit) {
    }

    record StatusRequest(@NotNull Boolean active) {
    }

    record CourseRequest(@NotBlank(message = "Informe o nome do curso.") @Size(max = 120) String name) {
    }

    @GetMapping
    List<InstitutionView> list(@Parameter(hidden = true) @CurrentActor Actor actor) {
        return institutions.list(actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InstitutionView create(@Parameter(hidden = true) @CurrentActor Actor actor,
            @Valid @RequestBody InstitutionRequest request) {
        return institutions.create(actor, request.toCommand());
    }

    @GetMapping("/{id}")
    InstitutionView get(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id) {
        return institutions.get(actor, id);
    }

    @PutMapping("/{id}")
    InstitutionView update(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody InstitutionRequest request) {
        return institutions.update(actor, id, request.toCommand());
    }

    @PatchMapping("/{id}/status")
    InstitutionView setStatus(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody StatusRequest request) {
        return institutions.setActive(actor, id, request.active());
    }

    @PatchMapping("/{id}/hours-limit")
    InstitutionView changeHoursLimit(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody HoursLimitRequest request) {
        return institutions.changeHoursLimit(actor, id, request.weeklyHoursLimit());
    }

    @GetMapping("/{id}/hours-summary")
    HoursSummary hoursSummary(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @RequestParam(required = false) Semester semester) {
        return institutions.hoursSummary(actor, id, semester);
    }

    @GetMapping("/{id}/courses")
    List<CourseView> courses(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id) {
        return courses.list(actor, id);
    }

    @PostMapping("/{id}/courses")
    @ResponseStatus(HttpStatus.CREATED)
    CourseView addCourse(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody CourseRequest request) {
        return courses.add(actor, id, request.name());
    }

    @DeleteMapping("/{id}/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeCourse(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @PathVariable UUID courseId) {
        courses.remove(actor, id, courseId);
    }
}
