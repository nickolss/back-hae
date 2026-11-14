package br.com.cps.hae.web.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.cps.hae.application.feedback.FeedbackService;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.web.security.CurrentActor;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/feedback")
@Tag(name = "Suporte")
class FeedbackController {

    private final FeedbackService feedback;

    FeedbackController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    record FeedbackRequest(@NotNull(message = "Selecione a categoria.") FeedbackService.Category category,
            @NotBlank(message = "Informe o assunto.") @Size(max = 150) String subject,
            @NotBlank(message = "Descreva sua mensagem.") @Size(max = 5000) String description) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    void send(@Parameter(hidden = true) @CurrentActor Actor actor, @Valid @RequestBody FeedbackRequest request) {
        feedback.send(actor, request.category(), request.subject(), request.description());
    }
}
