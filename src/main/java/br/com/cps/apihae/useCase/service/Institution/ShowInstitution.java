package br.com.cps.apihae.useCase.service.Institution;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cps.apihae.adapter.dto.response.InstitutionResponseDTO;
import br.com.cps.apihae.adapter.dto.response.InstitutionCourseResponseDTO;
import br.com.cps.apihae.domain.entity.Employee;
import br.com.cps.apihae.domain.entity.Hae;
import br.com.cps.apihae.domain.entity.Institution;
import br.com.cps.apihae.useCase.Interface.IEmployeeRepository;
import br.com.cps.apihae.useCase.Interface.IHaeRepository;
import br.com.cps.apihae.useCase.Interface.IInstitutionCourseRepository;
import br.com.cps.apihae.useCase.Interface.IInstitutionRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ShowInstitution {

    private final IInstitutionRepository iInstitutionRepository;
    private final IEmployeeRepository iEmployeeRepository;
    private final IHaeRepository iHaeRepository;
    private final IInstitutionCourseRepository iInstitutionCourseRepository;

    @Transactional(readOnly = true)
    public int getHaeQtdHours(String id) throws IllegalArgumentException {
        Institution inst = iInstitutionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com ID: " + id));
        return inst.getHaeQtd();
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponseDTO> listAllInstitutions() {
        List<Institution> institutions = iInstitutionRepository.findAll();

        return institutions.stream()
                .map(InstitutionResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByInstitutionId(String institutionId) {
        return iEmployeeRepository.findByInstitutionId(institutionId);
    }

    @Transactional(readOnly = true)
    public Institution getInstitutionByInstitutionCode(Integer institutionCode) {
        return iInstitutionRepository.findByInstitutionCode(institutionCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Instituição não encontrada com código: " + institutionCode));
    }

    @Transactional(readOnly = true)
    public List<Hae> getHaesByInstitutionId(String institutionId) {
        return iHaeRepository.findByInstitutionId(institutionId);
    }

    @Transactional(readOnly = true)
    public Institution getInstitutionById(String institutionId) {
        return iInstitutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Instituição não encontrada com ID: " + institutionId));
    }

    @Transactional(readOnly = true)
    public List<InstitutionCourseResponseDTO> getCoursesByInstitutionId(String institutionId) {
        return iInstitutionCourseRepository.findByInstitutionIdAndActiveTrueOrderByCourseNameAsc(institutionId)
                .stream()
                .map(InstitutionCourseResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InstitutionCourseResponseDTO> getCoursesByInstitutionCode(Integer institutionCode) {
        return iInstitutionCourseRepository
                .findByInstitutionInstitutionCodeAndActiveTrueOrderByCourseNameAsc(institutionCode)
                .stream()
                .map(InstitutionCourseResponseDTO::new)
                .collect(Collectors.toList());
    }

}
