package br.com.cps.hae.application.port;

import java.util.UUID;

import br.com.cps.hae.application.shared.PageQuery;
import br.com.cps.hae.application.shared.PageResult;
import br.com.cps.hae.domain.employee.Role;

public interface EmployeeQueries {

    PageResult<EmployeeSummary> search(Criteria criteria, PageQuery page);

    record Criteria(UUID institutionId, Role role, String text) {
    }

    record EmployeeSummary(UUID id, String name, String email, Role role, UUID institutionId,
            String institutionName, UUID courseId, String courseName, long haeCount) {
    }
}
