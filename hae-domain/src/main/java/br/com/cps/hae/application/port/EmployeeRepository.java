package br.com.cps.hae.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.shared.EmailAddress;

public interface EmployeeRepository {

    Optional<Employee> findById(UUID id);

    Optional<Employee> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);

    boolean existsByRole(Role role);

    boolean existsByCourse(UUID courseId);

    List<Employee> findByRoleAndCourse(Role role, UUID courseId);

    Employee save(Employee employee);

    void delete(UUID id);
}
