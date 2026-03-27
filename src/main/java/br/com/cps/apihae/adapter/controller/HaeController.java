package br.com.cps.apihae.adapter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.cps.apihae.adapter.dto.request.HaeClosureRequest;
import br.com.cps.apihae.adapter.dto.request.HaeRequest;
import br.com.cps.apihae.adapter.dto.request.HaeStatusUpdateRequest;
import br.com.cps.apihae.adapter.dto.response.HaeClosureRecordDTO;
import br.com.cps.apihae.adapter.dto.response.HaeDetailDTO;
import br.com.cps.apihae.adapter.dto.response.HaeHoursResponseDTO;
import br.com.cps.apihae.adapter.dto.response.HaeResponseDTO;
import br.com.cps.apihae.adapter.facade.HaeFacade;
import br.com.cps.apihae.domain.entity.Employee;
import br.com.cps.apihae.domain.entity.Hae;
import br.com.cps.apihae.domain.enums.HaeType;
import br.com.cps.apihae.domain.enums.Role;
import br.com.cps.apihae.domain.enums.Status;
import br.com.cps.apihae.useCase.Interface.IEmployeeRepository;
import br.com.cps.apihae.useCase.util.JWTUtils;
import jakarta.validation.Valid;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hae")
@SecurityRequirement(name = "cookieAuth")
@RequiredArgsConstructor
@Tag(name = "Hae", description = "Endpoints para manipular HAEs (Horas de Atividades Específicas)")
public class HaeController {
    private final HaeFacade haeFacade;
    private final JWTUtils jwtUtils;
    private final IEmployeeRepository employeeRepository;

    @PostMapping("/create")
    public ResponseEntity<Object> createHae(@Valid @RequestBody HaeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(haeFacade.createHae(request));
    }

    @GetMapping("/getHaeById/{id}")
    public ResponseEntity<HaeDetailDTO> getHaeById(
            @PathVariable String id,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        Employee authenticatedEmployee = getAuthenticatedEmployee(authToken);
        String userInstitutionId = isGlobalAccessRole(authenticatedEmployee.getRole())
                ? null
                : authenticatedEmployee.getInstitution().getId();
        return ResponseEntity.ok(haeFacade.getHaeById(id, userInstitutionId));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Object> deleteHae(@PathVariable String id) {
        haeFacade.deleteHae(id);
        return ResponseEntity.ok(Collections.singletonMap("mensagem", "HAE deletado com sucesso."));
    }

    @GetMapping("/employee/{id}")
    public ResponseEntity<List<Hae>> getHaesByEmployeeId(
            @PathVariable String id,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        assertEmployeeInstitutionScope(id, authToken);
        return ResponseEntity.ok(haeFacade.getHaesByEmployeeId(id));
    }

    @GetMapping("/getHaesByEmployeeIdWithLimit/{employeeId}")
    public ResponseEntity<List<Hae>> getHaesByEmployeeIdWithLimit(
            @PathVariable String employeeId,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        assertEmployeeInstitutionScope(employeeId, authToken);
        return ResponseEntity.ok(haeFacade.getHaesByEmployeeIdWithLimit(employeeId));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<HaeResponseDTO>> getAllHaes(
            @CookieValue(value = "auth_token", required = false) String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return ResponseEntity.ok(haeFacade.getAllHaes());
        }

        String scopeInstitutionId;
        try {
            scopeInstitutionId = getScopeInstitutionId(authToken);
        } catch (SecurityException ex) {
            return ResponseEntity.ok(haeFacade.getAllHaes());
        }

        if (scopeInstitutionId == null) {
            return ResponseEntity.ok(haeFacade.getAllHaes());
        }

        return ResponseEntity.ok(haeFacade.getHaesByInstitutionId(scopeInstitutionId));
    }

    @PutMapping("/change-status/{id}")
    public ResponseEntity<Object> changeHaeStatus(@PathVariable String id,
            @Valid @RequestBody HaeStatusUpdateRequest request) {
        return ResponseEntity.ok(haeFacade.updateHaeStatus(id, request.getNewStatus(), request.getCoordenadorId()));
    }

    @GetMapping("/getHaesByProfessor/{professorId}")
    public ResponseEntity<List<HaeResponseDTO>> getHaesByProfessor(
            @PathVariable String professorId,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return ResponseEntity.ok(haeFacade.getHaesByProfessorId(professorId));
        }

        try {
            assertEmployeeInstitutionScope(professorId, authToken);
        } catch (SecurityException ex) {
            return ResponseEntity.ok(haeFacade.getHaesByProfessorId(professorId));
        }

        return ResponseEntity.ok(haeFacade.getHaesByProfessorId(professorId));
    }

    @GetMapping("/getHaesByCourse/{course}")
    public ResponseEntity<List<HaeResponseDTO>> getHaesByCourse(
            @PathVariable String course,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String scopeInstitutionId = getScopeInstitutionId(authToken);
        if (scopeInstitutionId == null) {
            return ResponseEntity.ok(haeFacade.getHaesByCourse(course));
        }

        return ResponseEntity.ok(haeFacade.advancedHaeSearch(scopeInstitutionId, course, null, null, null));
    }

    @GetMapping("/getHaesByType/{haeType}")
    public ResponseEntity<?> getHaesByType(
            @PathVariable HaeType haeType,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String scopeInstitutionId = getScopeInstitutionId(authToken);
        if (scopeInstitutionId == null) {
            return ResponseEntity.ok(haeFacade.getHaesByType(haeType));
        }

        List<Hae> filtered = haeFacade.getHaesByType(haeType).stream()
                .filter(hae -> hae.getInstitution() != null
                        && scopeInstitutionId.equals(hae.getInstitution().getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(filtered);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Hae> updateHae(@PathVariable String id, @RequestBody HaeRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(haeFacade.updateHae(id, request));
    }

    @PostMapping("/createHaeAsCoordinator/{coordinatorId}/{employeeId}")
    public ResponseEntity<?> createHaeAsCoordinator(@PathVariable String coordinatorId, @PathVariable String employeeId,
            @Valid @RequestBody HaeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(haeFacade.createHaeAsCoordinator(coordinatorId, employeeId, request));
    }

    @Operation(summary = "Alternar status de visualização", description = "Alterna o status 'viewed' de uma HAE (de true para false e vice-versa).")
    @PutMapping("/viewed/toggle/{haeId}")
    public ResponseEntity<Hae> toggleViewed(@PathVariable String haeId) {
        try {
            return ResponseEntity.ok(haeFacade.toggleViewedStatus(haeId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(summary = "Listar HAEs visualizadas", description = "Retorna todas as HAEs que já foram visualizadas")
    @GetMapping("/viewed")
    public ResponseEntity<List<Hae>> getViewed(
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String scopeInstitutionId = getScopeInstitutionId(authToken);
        if (scopeInstitutionId == null) {
            return ResponseEntity.ok(haeFacade.getViewed());
        }

        List<Hae> filtered = haeFacade.getViewed().stream()
                .filter(hae -> hae.getInstitution() != null
                        && scopeInstitutionId.equals(hae.getInstitution().getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(filtered);
    }

    @Operation(summary = "Listar HAEs não visualizadas", description = "Retorna todas as HAEs que ainda não foram visualizadas")
    @GetMapping("/not-viewed")
    public ResponseEntity<List<Hae>> getNotViewed(
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String scopeInstitutionId = getScopeInstitutionId(authToken);
        if (scopeInstitutionId == null) {
            return ResponseEntity.ok(haeFacade.getNotViewed());
        }

        List<Hae> filtered = haeFacade.getNotViewed().stream()
                .filter(hae -> hae.getInstitution() != null
                        && scopeInstitutionId.equals(hae.getInstitution().getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(filtered);
    }

    @Operation(summary = "Lista todas as HAEs de uma instituição", description = "Retorna uma lista de HAEs baseada no ID da instituição fornecido.")
    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<HaeResponseDTO>> getHaesByInstitution(
            @PathVariable String institutionId,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String scopeInstitutionId = getScopeInstitutionId(authToken);
        String effectiveInstitutionId = scopeInstitutionId == null ? institutionId : scopeInstitutionId;
        return ResponseEntity.ok(haeFacade.getHaesByInstitutionId(effectiveInstitutionId));
    }

    @Operation(summary = "Lista todas as HAEs baseado nos status", description = "Retorna uma lista de HAEs baseado no status fornecido.")
    @GetMapping("/getHaeByStatus/{status}")
    public ResponseEntity<List<HaeResponseDTO>> getHaesByStatus(
            @PathVariable Status status,
            @CookieValue(value = "auth_token", required = false) String authToken) {
        String scopeInstitutionId = getScopeInstitutionId(authToken);
        if (scopeInstitutionId == null) {
            return ResponseEntity.ok(haeFacade.getHaeByStatus(status));
        }

        return ResponseEntity.ok(haeFacade.advancedHaeSearch(scopeInstitutionId, null, null, status, null));
    }

    @Operation(summary = "Busca avançada de HAEs", description = "Filtra HAEs por instituição, curso, tipo, status e visualização. Todos os filtros são opcionais.")
    @GetMapping("/search")
    public ResponseEntity<List<HaeResponseDTO>> searchHaes(
            @RequestParam(required = false) String institutionId,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) HaeType haeType,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Boolean viewed,
            @CookieValue(value = "auth_token", required = false) String authToken) {

        String scopeInstitutionId = getScopeInstitutionId(authToken);
        String effectiveInstitutionId = scopeInstitutionId == null ? institutionId : scopeInstitutionId;

        return ResponseEntity.ok(haeFacade.advancedHaeSearch(effectiveInstitutionId, course, haeType, status, viewed));
    }

    @GetMapping("/getAllWeeklyHours")
    public ResponseEntity<?> getAllWeeklyHours() {
        return ResponseEntity.ok(haeFacade.getAllWeeklyHours());
    }

    @GetMapping("/getWeeklyHoursByHae/{haeId}")
    public ResponseEntity<HaeHoursResponseDTO> getWeeklyHoursByHae(@PathVariable String haeId) {
        return ResponseEntity.ok(haeFacade.getWeeklyHoursByHae(haeId));
    }

    @Operation(summary = "Solicitar fechamento de HAE", description = "Envia as informações de fechamento de uma HAE baseado no tipo")
    @PostMapping("/request-closure/{haeId}")
    public ResponseEntity<Object> requestClosure(@PathVariable String haeId,
            @Valid @RequestBody HaeClosureRequest request) {
        return ResponseEntity.ok(haeFacade.requestClosure(haeId, request));
    }

    @Operation(summary = "Buscar histórico de fechamento de HAE", description = "Retorna todos os registros de fechamento aprovado de uma HAE")
    @GetMapping("/closure-records/{haeId}")
    public ResponseEntity<List<HaeClosureRecordDTO>> getClosureRecords(@PathVariable String haeId) {
        return ResponseEntity.ok(haeFacade.getClosureRecordsByHaeId(haeId));
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

    private String getScopeInstitutionId(String authToken) {
        Employee authenticatedEmployee = getAuthenticatedEmployee(authToken);
        if (isGlobalAccessRole(authenticatedEmployee.getRole())) {
            return null;
        }

        return authenticatedEmployee.getInstitution().getId();
    }

    private void assertEmployeeInstitutionScope(String targetEmployeeId, String authToken) {
        String scopeInstitutionId = getScopeInstitutionId(authToken);
        if (scopeInstitutionId == null) {
            return;
        }

        Employee targetEmployee = employeeRepository.findById(targetEmployeeId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário alvo não encontrado."));

        if (targetEmployee.getInstitution() == null
                || !scopeInstitutionId.equals(targetEmployee.getInstitution().getId())) {
            throw new IllegalArgumentException("Acesso negado para usuários de outra instituição.");
        }
    }

}