package com.example.employees.employee;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByEmployeeIdIgnoreCaseAndIdNot(String employeeId, Long id);
    long countByStatus(EmploymentStatus status);

    @Query("select count(distinct lower(e.department)) from Employee e")
    long countDepartments();

    @Query("select min(e.department) from Employee e group by lower(e.department) order by lower(e.department)")
    List<String> findDepartments();
}
