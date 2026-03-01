package br.com.cps.apihae.useCase.service.Employee;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import br.com.cps.apihae.adapter.dto.request.InstitutionRequestDTO;
import br.com.cps.apihae.adapter.dto.response.EmployeeResponseDTO;
import br.com.cps.apihae.domain.entity.Employee;

@RequiredArgsConstructor
@Service
public class ConvertDTOEmployee {

    public static EmployeeResponseDTO convertToDto(Employee employee) {
        InstitutionRequestDTO institutionDto = null;
        if (employee.getInstitution() != null) {
            institutionDto = new InstitutionRequestDTO(
                    employee.getInstitution().getId(),
                    employee.getInstitution().getName(),
                    employee.getInstitution().getInstitutionCode());
        }

        return new EmployeeResponseDTO(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getCourse(),
                employee.getRole(),
                institutionDto);
    }
}
