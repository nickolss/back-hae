package br.com.cps.apihae.useCase.service.Employee;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cps.apihae.adapter.dto.response.EmployeeResponseDTO;
import br.com.cps.apihae.adapter.dto.response.EmployeeSummaryDTO;
import br.com.cps.apihae.domain.entity.Employee;
import br.com.cps.apihae.domain.enums.Role;
import br.com.cps.apihae.useCase.Interface.IEmployeeRepository;
import br.com.cps.apihae.useCase.Interface.IHaeRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
@Service
public class ShowEmployee {

    private final IEmployeeRepository iEmployeeRepository;
    private final IHaeRepository iHaeRepository;

    @Transactional(readOnly = true)
    public Page<EmployeeResponseDTO> getAllEmployees(Pageable pageable, String name) {
        Page<Employee> employees;

        if (name != null && !name.isBlank()) {
            employees = iEmployeeRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            employees = iEmployeeRepository.findAll(pageable);
        }

        return employees.map(EmployeeResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public List<EmployeeSummaryDTO> getEmployeeSummaries(Role role) {
        List<Employee> professors = iEmployeeRepository.findAllByRole(role);

        return professors.stream().map(professor -> {
            int haeCount = iHaeRepository.countByEmployeeId(professor.getId());

            return new EmployeeSummaryDTO(
                    professor.getId(),
                    professor.getName(),
                    professor.getEmail(),
                    professor.getCourse(),
                    haeCount);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeByEmail(String email) {
        Employee employee = iEmployeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Empregado não encontrado com email " + email));
        return ConvertDTOEmployee.convertToDto(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeById(String id) {
        Employee employee = iEmployeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empregado não encontrado com ID: " + id));
        return ConvertDTOEmployee.convertToDto(employee);
    }

    public List<Employee> getEmployeesByInstitutionId(String institutionId) {
        return iEmployeeRepository.findByInstitutionId(institutionId);
    }
}
