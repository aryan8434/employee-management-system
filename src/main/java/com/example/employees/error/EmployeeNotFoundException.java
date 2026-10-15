package com.example.employees.error;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(Long id) {
        super("Employee " + id + " was not found.");
    }
}
