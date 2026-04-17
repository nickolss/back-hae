package br.com.cps.apihae.useCase.Interface;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.cps.apihae.domain.entity.Employee;
import br.com.cps.apihae.domain.enums.Role;

public interface IEmployeeRepository {
    Optional<Employee> findByEmail(String email);

    List<Employee> findAllByRole(Role role);

    List<Employee> findByInstitutionId(String institutionId);

    Optional<Employee> findByCourseAndRole(String course, Role role);

    Employee save(Employee employee);

    Optional<Employee> findById(String id);

    void delete(Employee employee);

    void deleteById(String id);

    List<Employee> findAll();

    Page<Employee> findAll(Pageable pageable);

    Page<Employee> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsById(String id);
}
