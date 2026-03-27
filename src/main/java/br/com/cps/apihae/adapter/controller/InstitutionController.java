package br.com.cps.apihae.adapter.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.cps.apihae.adapter.dto.request.InstitutionCreateRequest;
import br.com.cps.apihae.adapter.dto.request.InstitutionCourseRequest;
import br.com.cps.apihae.adapter.dto.request.InstitutionUpdateRequest;
import br.com.cps.apihae.adapter.dto.response.InstitutionCourseResponseDTO;
import br.com.cps.apihae.adapter.dto.response.InstitutionResponseDTO;
import br.com.cps.apihae.adapter.facade.InstitutionFacade;
import br.com.cps.apihae.domain.entity.Employee;
import br.com.cps.apihae.domain.entity.Institution;
import br.com.cps.apihae.domain.entity.InstitutionCourse;
import br.com.cps.apihae.domain.enums.Role;
import br.com.cps.apihae.useCase.Interface.IEmployeeRepository;
import br.com.cps.apihae.useCase.util.JWTUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/institution")
public class InstitutionController {
    private final InstitutionFacade institutionFacade;
    private final JWTUtils jwtUtils;
    private final IEmployeeRepository employeeRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createInstitution(@RequestBody InstitutionCreateRequest request) {
        institutionFacade.createInstitution(request);
        return ResponseEntity.ok("Instituição criada com sucesso!");
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Institution> updateInstitution(@PathVariable Integer id,
            @Valid @RequestBody InstitutionUpdateRequest request) {
        return ResponseEntity.ok(institutionFacade.updateInstitution(id, request));
    }

    @GetMapping("/getAvailableHaesCount")
    public ResponseEntity<Integer> getAvailableHaesCount(
            @RequestParam String institutionId,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String effectiveInstitutionId = getEffectiveInstitutionId(institutionId, authToken);
        return ResponseEntity.ok(institutionFacade.getAvailableHaesCount(effectiveInstitutionId));
    }

    @PostMapping("/setAvailableHaesCount")
    public ResponseEntity<?> setAvailableHaesCount(@RequestParam int count, @RequestParam String userId,
            @RequestParam String institutionId) {
        institutionFacade.setAvailableHaesCount(count, userId, institutionId);
        return ResponseEntity.ok("Quantidade de HAEs disponíveis atualizada para: " + count);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<InstitutionResponseDTO>> getAllInstitutions() {
        return ResponseEntity.ok(institutionFacade.getAllInstitutions());
    }

    @GetMapping("/getInstitutionById")
    public ResponseEntity<?> getInstitutionById(
            @RequestParam String institutionId,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String effectiveInstitutionId = getEffectiveInstitutionId(institutionId, authToken);
        return ResponseEntity.ok(institutionFacade.getInstitutionById(effectiveInstitutionId));
    }

    @GetMapping("/getInstitutionByInstitutionCode")
    public ResponseEntity<?> getInstitutionByInstitutionCode(@RequestParam Integer institutionCode) {
        return ResponseEntity.ok(institutionFacade.getInstitutionByInstitutionCode(institutionCode));
    }

    @GetMapping("/getEmployeesByInstitutionId")
    public ResponseEntity<?> getEmployeesByInstitutionId(
            @RequestParam String institutionId,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String effectiveInstitutionId = getEffectiveInstitutionId(institutionId, authToken);
        return ResponseEntity.ok(institutionFacade.getEmployeesByInstitutionId(effectiveInstitutionId));
    }

    @GetMapping("/getHaesByInstitutionId")
    public ResponseEntity<?> getHaesByInstitutionId(
            @RequestParam String institutionId,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String effectiveInstitutionId = getEffectiveInstitutionId(institutionId, authToken);
        return ResponseEntity.ok(institutionFacade.getHaesByInstitutionId(effectiveInstitutionId));
    }

    @GetMapping("/getRemainingHours")
    public ResponseEntity<Integer> getRemainingHours(
            @RequestParam String institutionId,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String effectiveInstitutionId = getEffectiveInstitutionId(institutionId, authToken);
        int remainingHours = institutionFacade.getRemainingHours(effectiveInstitutionId);
        return ResponseEntity.ok(remainingHours);
    }

    @GetMapping("/getCoursesByInstitutionId")
    public ResponseEntity<List<InstitutionCourseResponseDTO>> getCoursesByInstitutionId(
            @RequestParam String institutionId,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String effectiveInstitutionId = getEffectiveInstitutionId(institutionId, authToken);
        return ResponseEntity.ok(institutionFacade.getCoursesByInstitutionId(effectiveInstitutionId));
    }

    @GetMapping("/getCoursesByInstitutionCode")
    public ResponseEntity<List<InstitutionCourseResponseDTO>> getCoursesByInstitutionCode(
            @RequestParam Integer institutionCode,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        Integer effectiveInstitutionCode = (authToken == null || authToken.isBlank())
                ? institutionCode
                : getEffectiveInstitutionCode(institutionCode, authToken);
        return ResponseEntity.ok(institutionFacade.getCoursesByInstitutionCode(effectiveInstitutionCode));
    }

    @PostMapping("/course")
    public ResponseEntity<InstitutionCourse> addInstitutionCourse(
            @Valid @RequestBody InstitutionCourseRequest request,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        assertCanManageCourses(authToken);
        return ResponseEntity.ok(institutionFacade.addInstitutionCourse(request));
    }

    @DeleteMapping("/course/{courseId}")
    public ResponseEntity<?> removeInstitutionCourse(
            @PathVariable String courseId,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        assertCanManageCourses(authToken);
        institutionFacade.removeInstitutionCourse(courseId);
        return ResponseEntity.ok("Curso removido com sucesso.");
    }

    private String getEffectiveInstitutionId(String requestedInstitutionId, String authToken) {
        Employee authenticatedEmployee = getAuthenticatedEmployee(authToken);
        if (isGlobalAccessRole(authenticatedEmployee.getRole())) {
            return requestedInstitutionId;
        }

        return authenticatedEmployee.getInstitution().getId();
    }

    private Integer getEffectiveInstitutionCode(Integer requestedInstitutionCode, String authToken) {
        Employee authenticatedEmployee = getAuthenticatedEmployee(authToken);
        if (isGlobalAccessRole(authenticatedEmployee.getRole())) {
            return requestedInstitutionCode;
        }

        return authenticatedEmployee.getInstitution().getInstitutionCode();
    }

    private Employee getAuthenticatedEmployee(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            throw new SecurityException("Usuário não autenticado.");
        }

        String employeeId = jwtUtils.validateToken(authToken);
        if (employeeId == null || employeeId.isBlank()) {
            throw new SecurityException("Token de autenticação inválido ou expirado.");
        }

        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new SecurityException("Usuário autenticado não encontrado."));
    }

    private boolean isGlobalAccessRole(Role role) {
        return role == Role.ADMIN || role == Role.DEV;
    }

    private void assertCanManageCourses(String authToken) {
        Employee authenticatedEmployee = getAuthenticatedEmployee(authToken);
        if (authenticatedEmployee.getRole() != Role.DEV && authenticatedEmployee.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Acesso negado para gerenciar cursos.");
        }
    }
}
