package br.com.cps.apihae.useCase.service.Institution;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cps.apihae.adapter.dto.request.InstitutionCreateRequest;
import br.com.cps.apihae.adapter.dto.request.InstitutionCourseRequest;
import br.com.cps.apihae.adapter.dto.request.InstitutionUpdateRequest;
import br.com.cps.apihae.adapter.dto.response.EmployeeResponseDTO;
import br.com.cps.apihae.domain.entity.InstitutionCourse;
import br.com.cps.apihae.domain.entity.Institution;
import br.com.cps.apihae.domain.enums.Role;
import br.com.cps.apihae.domain.factory.InstitutionFactory;
import br.com.cps.apihae.useCase.Interface.IInstitutionCourseRepository;
import br.com.cps.apihae.useCase.Interface.IInstitutionRepository;
import br.com.cps.apihae.useCase.service.Employee.ShowEmployee;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ManageInstitution {

    private final IInstitutionRepository iInstitutionRepository;
    private final IInstitutionCourseRepository iInstitutionCourseRepository;
    private final ShowEmployee showEmployee;

    public void createInstitution(InstitutionCreateRequest request) {
        iInstitutionRepository.findByName(request.getName()).ifPresent(inst -> {
            throw new IllegalArgumentException("Nome de instituição já em uso.");
        });

        iInstitutionRepository.findByInstitutionCode(request.getInstitutionCode()).ifPresent(inst -> {
            throw new IllegalArgumentException("Código de instituição já em uso.");
        });

        Institution institution = InstitutionFactory.create(request);

        iInstitutionRepository.save(institution);
    }

    @Transactional()
    public void setHaeQtd(int quantidade, String userId, String institutionId) {
        Optional.of(quantidade).filter(q -> q >= 0)
                .orElseThrow(() -> new IllegalArgumentException("Quantidade não pode ser negativa."));

        EmployeeResponseDTO employee = showEmployee.getEmployeeById(userId);
        if (employee == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        if (employee.getRole() != Role.DIRETOR && employee.getRole() != Role.ADMIN && employee.getRole() != Role.DEV) {
            throw new IllegalArgumentException(
                    "Usuário não é um diretor ou administrador da unidade, impossivel definir a quantidade de HAEs.");
        }

        Institution institution = iInstitutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com ID: " + institutionId));
        institution.setHaeQtd(quantidade);
        iInstitutionRepository.save(institution);

    }

    @Transactional()
    public Institution updateInstitution(Integer id, InstitutionUpdateRequest request) {
        Institution institution = iInstitutionRepository.findByInstitutionCode(id)
                .orElseThrow(() -> new IllegalArgumentException("Instituição não encontrada com código: " + id));

        iInstitutionRepository.findByName(request.getName())
                .filter(existing -> !existing.getId().equals(institution.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "O nome '" + request.getName() + "' já está em uso por outra instituição.");
                });

        Optional<Institution> existingByCode = iInstitutionRepository
                .findByInstitutionCode(request.getInstitutionCode())
                .filter(existing -> !existing.getId().equals(institution.getId()));

        existingByCode.ifPresent(existing -> {
            throw new IllegalArgumentException("O código '" + request.getInstitutionCode() + "' já está em uso.");
        });

        institution.setName(request.getName());
        institution.setAddress(request.getAddress());
        institution.setHaeQtd(request.getHaeQtd());
        institution.setInstitutionCode(request.getInstitutionCode());

        return iInstitutionRepository.save(institution);
    }

    @Transactional
    public InstitutionCourse addInstitutionCourse(InstitutionCourseRequest request) {
        String normalizedCourseName = request.getCourseName().trim();
        if (normalizedCourseName.isBlank()) {
            throw new IllegalArgumentException("Nome do curso inválido.");
        }

        Institution institution = iInstitutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new IllegalArgumentException("Instituição não encontrada."));

        iInstitutionCourseRepository
                .findByInstitutionIdAndCourseNameIgnoreCase(request.getInstitutionId(), normalizedCourseName)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Curso já cadastrado para esta instituição.");
                });

        InstitutionCourse institutionCourse = new InstitutionCourse();
        institutionCourse.setInstitution(institution);
        institutionCourse.setCourseName(normalizedCourseName);
        institutionCourse.setActive(true);

        return iInstitutionCourseRepository.save(institutionCourse);
    }

    @Transactional
    public void removeInstitutionCourse(String courseId) {
        InstitutionCourse institutionCourse = iInstitutionCourseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));

        iInstitutionCourseRepository.delete(institutionCourse);
    }

}
