package br.com.cps.hae.web.api;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.cps.hae.application.hae.AttachmentService;
import br.com.cps.hae.application.hae.HaeQueryService;
import br.com.cps.hae.application.hae.HaeService;
import br.com.cps.hae.application.port.HaeQueries;
import br.com.cps.hae.application.port.HaeQueries.HaeSummary;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.PageQuery;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.Semester;
import br.com.cps.hae.web.api.HaeDtos.ClosureRequest;
import br.com.cps.hae.web.api.HaeDtos.HaeRequest;
import br.com.cps.hae.web.api.HaeDtos.HaeResponse;
import br.com.cps.hae.web.api.HaeDtos.ReviewRequest;
import br.com.cps.hae.web.api.HaeDtos.ViewedRequest;
import br.com.cps.hae.web.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/haes")
@Tag(name = "HAEs")
class HaeController {

    private final HaeService commands;
    private final HaeQueryService queries;
    private final AttachmentService attachments;

    HaeController(HaeService commands, HaeQueryService queries, AttachmentService attachments) {
        this.commands = commands;
        this.queries = queries;
        this.attachments = attachments;
    }

    @Operation(summary = "Busca HAEs visíveis ao usuário (escopo aplicado automaticamente)")
    @GetMapping
    PageResponse<HaeSummary> search(@Parameter(hidden = true) @CurrentActor Actor actor,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) HaeType type,
            @RequestParam(required = false) HaeStatus status,
            @RequestParam(required = false) Boolean viewed,
            @RequestParam(required = false) Semester semester,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var criteria = new HaeQueries.Criteria(institutionId, courseId, employeeId, type, status, viewed, semester, q);
        return PageResponse.of(queries.search(actor, criteria, new PageQuery(page, size)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    HaeResponse create(@Parameter(hidden = true) @CurrentActor Actor actor, @Valid @RequestBody HaeRequest request) {
        return HaeResponse.of(commands.create(actor,
                new HaeService.CreateHaeCommand(request.employeeId(), request.courseId(), request.toDetails())));
    }

    @GetMapping("/{id}")
    HaeResponse get(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id) {
        return HaeResponse.of(queries.get(actor, id));
    }

    @PutMapping("/{id}")
    HaeResponse update(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody HaeRequest request) {
        return HaeResponse.of(commands.update(actor, id, request.courseId(), request.toDetails()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id) {
        commands.delete(actor, id);
    }

    @Operation(summary = "Aprova ou reprova uma HAE pendente")
    @PostMapping("/{id}/review")
    HaeResponse review(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request) {
        return HaeResponse.of(commands.review(actor, id, request.decision(), request.comment()));
    }

    @PatchMapping("/{id}/viewed")
    HaeResponse markViewed(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody ViewedRequest request) {
        return HaeResponse.of(commands.markViewed(actor, id, request.viewed()));
    }

    @Operation(summary = "Envia o relatório de fechamento de uma HAE aprovada")
    @PostMapping("/{id}/closure")
    HaeResponse requestClosure(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody ClosureRequest request) {
        return HaeResponse.of(commands.requestClosure(actor, id, request.toReport()));
    }

    @Operation(summary = "Aprova ou recusa o fechamento pendente")
    @PostMapping("/{id}/closure/review")
    HaeResponse reviewClosure(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request) {
        return HaeResponse.of(commands.reviewClosure(actor, id, request.decision(), request.comment()));
    }

    @Operation(summary = "Anexa um comprovante (PDF, PNG ou JPG, até 5 MB)")
    @PostMapping(path = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    HaeResponse upload(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @RequestPart("file") MultipartFile file) {
        try {
            return HaeResponse.of(attachments.upload(actor, id,
                    new AttachmentService.Upload(file.getOriginalFilename(), file.getBytes())));
        } catch (IOException e) {
            throw new BusinessRuleViolationException("Não foi possível ler o arquivo enviado.");
        }
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    HaeResponse removeAttachment(@Parameter(hidden = true) @CurrentActor Actor actor, @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        return HaeResponse.of(commands.removeAttachment(actor, id, attachmentId));
    }
}
