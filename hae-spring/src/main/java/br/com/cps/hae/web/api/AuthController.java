package br.com.cps.hae.web.api;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.cps.hae.application.account.EmployeeProfile;
import br.com.cps.hae.application.auth.AuthenticatedSession;
import br.com.cps.hae.application.auth.AuthenticationService;
import br.com.cps.hae.application.auth.PasswordRecoveryService;
import br.com.cps.hae.application.auth.RegistrationService;
import br.com.cps.hae.web.security.SessionCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
class AuthController {

    private final RegistrationService registration;
    private final AuthenticationService authentication;
    private final PasswordRecoveryService recovery;
    private final SessionCookies cookies;

    AuthController(RegistrationService registration, AuthenticationService authentication,
            PasswordRecoveryService recovery, SessionCookies cookies) {
        this.registration = registration;
        this.authentication = authentication;
        this.recovery = recovery;
        this.cookies = cookies;
    }

    record RegisterRequest(
            @NotBlank(message = "Informe seu nome.") @Size(max = 100, message = "Máximo de 100 caracteres.") String name,
            @NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe uma senha.") @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.") String password,
            @NotNull(message = "Selecione a unidade.") @Positive Integer institutionCode,
            @NotNull(message = "Selecione o curso.") UUID courseId) {
    }

    record LoginRequest(@NotBlank(message = "Informe seu e-mail.") String email,
            @NotBlank(message = "Informe sua senha.") String password) {
    }

    record TokenRequest(@NotBlank @Size(max = 200) String token) {
    }

    record ForgotPasswordRequest(@NotBlank(message = "Informe seu e-mail.") String email) {
    }

    record ResetPasswordRequest(@NotBlank @Size(max = 200) String token,
            @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.") String newPassword) {
    }

    @Operation(summary = "Obtém o cookie XSRF-TOKEN (chamar antes do primeiro POST)")
    @GetMapping("/csrf")
    ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Inicia o cadastro e envia o e-mail de confirmação")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        registration.register(new RegistrationService.RegisterCommand(request.name(), request.email(),
                request.password(), request.institutionCode(), request.courseId()));
        return Map.of("message",
                "Se os dados estiverem corretos, você receberá um e-mail para ativar a conta em instantes.");
    }

    @Operation(summary = "Confirma o e-mail e inicia a sessão")
    @PostMapping("/activate")
    EmployeeProfile activate(@Valid @RequestBody TokenRequest request, HttpServletResponse response) {
        return startSession(registration.activate(request.token()), response);
    }

    @PostMapping("/login")
    EmployeeProfile login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return startSession(authentication.login(request.email(), request.password()), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletResponse response) {
        cookies.clear(response);
    }

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        recovery.requestReset(request.email());
        return Map.of("message", "Se o e-mail estiver cadastrado, você receberá um link para redefinir a senha.");
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        recovery.reset(request.token(), request.newPassword());
    }

    private EmployeeProfile startSession(AuthenticatedSession session, HttpServletResponse response) {
        cookies.write(response, session.token());
        return session.profile();
    }
}
