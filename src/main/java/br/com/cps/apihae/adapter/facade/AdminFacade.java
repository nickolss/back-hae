package br.com.cps.apihae.adapter.facade;


import org.springframework.stereotype.Component;

import br.com.cps.apihae.domain.entity.Employee;
import br.com.cps.apihae.domain.enums.Role;
import br.com.cps.apihae.useCase.service.Employee.ManageEmployee;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class AdminFacade {
    private final ManageEmployee manageEmployee;

    public Employee changeEmployeeRole(String id, Role newRole) {
        return manageEmployee.changeEmployeeRole(id, newRole);
    }
}
