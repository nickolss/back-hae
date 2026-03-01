package br.com.cps.apihae.adapter.dto.response;

import br.com.cps.apihae.adapter.dto.request.InstitutionRequestDTO;
import br.com.cps.apihae.domain.entity.Employee;
import br.com.cps.apihae.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDTO {
    private String id;
    private String name;
    private String email;
    private String course;
    private Role role;
    private InstitutionRequestDTO institution;

    public EmployeeResponseDTO(Employee employee) {
        this.id = employee.getId();
        this.name = employee.getName();
        this.email = employee.getEmail();
        this.course = employee.getCourse();
        this.role = employee.getRole();
        this.institution = new InstitutionRequestDTO(employee.getInstitution());
    }

}