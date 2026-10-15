package com.example.employees.employee;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, length = 20)
    private String employeeId;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 60)
    private String department;

    @Column(nullable = false, length = 80)
    private String designation;

    @Column(nullable = false)
    private LocalDate dateOfJoining;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EmploymentStatus status;

    protected Employee() {}

    public Employee(EmployeeRequest request) {
        update(request);
    }

    public void update(EmployeeRequest request) {
        employeeId = request.employeeId();
        fullName = request.fullName();
        email = request.email();
        phone = request.phone();
        department = request.department();
        designation = request.designation();
        dateOfJoining = request.dateOfJoining();
        salary = request.salary();
        status = request.status();
    }

    public Long getId() { return id; }
    public String getEmployeeId() { return employeeId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getDepartment() { return department; }
    public String getDesignation() { return designation; }
    public LocalDate getDateOfJoining() { return dateOfJoining; }
    public BigDecimal getSalary() { return salary; }
    public EmploymentStatus getStatus() { return status; }
}
