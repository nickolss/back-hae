package br.com.cps.hae.web.error;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import br.com.cps.hae.application.shared.AuthenticationFailedException;
import br.com.cps.hae.domain.shared.AccessDeniedException;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.ConflictException;
import br.com.cps.hae.domain.shared.DomainException;
import br.com.cps.hae.domain.shared.NotFoundException;

/**
 * Traduz exceções em Problem Details (RFC 9457). Mensagens de domínio são seguras para o usuário; erros
 * inesperados retornam mensagem genérica e são registrados no log (sem stack trace na resposta).
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException ex) {
        HttpStatus status = switch (ex) {
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case ConflictException ignored -> HttpStatus.CONFLICT;
            case AccessDeniedException ignored -> HttpStatus.FORBIDDEN;
            case AuthenticationFailedException ignored -> HttpStatus.UNAUTHORIZED;
            case BusinessRuleViolationException ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return problem(status, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Verifique os campos destacados.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class })
    ProblemDetail badRequest(Exception ex) {
        log.debug("Requisição inválida", ex);
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida. Verifique os dados enviados.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "O arquivo deve ter no máximo 5 MB.");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT,
                "Este registro foi alterado por outra pessoa. Recarregue a página e tente novamente.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "A operação conflita com dados existentes.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail notFound(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente em instantes.");
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
