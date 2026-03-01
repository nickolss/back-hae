package br.com.cps.apihae.adapter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cps.apihae.domain.entity.Employee;
import br.com.cps.apihae.useCase.Interface.IEmployeeRepository;

public interface EmployeeRepository extends IEmployeeRepository,JpaRepository<Employee, String> {
}
