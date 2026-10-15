package com.example.employees.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResponse(Long id, String employeeId, String fullName, String email,
        String phone, String department, String designation, LocalDate dateOfJoining,
        BigDecimal salary, EmploymentStatus status) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getEmployeeId(), employee.getFullName(),
                employee.getEmail(), employee.getPhone(), employee.getDepartment(), employee.getDesignation(),
                employee.getDateOfJoining(), employee.getSalary(), employee.getStatus());
    }
}
