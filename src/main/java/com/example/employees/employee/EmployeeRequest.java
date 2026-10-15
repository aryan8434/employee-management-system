package com.example.employees.employee;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public record EmployeeRequest(
        @NotBlank(message = "Enter an employee ID.")
        @Pattern(regexp = "[A-Z0-9][A-Z0-9-]{0,19}", message = "Use up to 20 letters, numbers or hyphens.")
        String employeeId,

        @NotBlank(message = "Enter a full name.")
        @Size(max = 100, message = "Use 100 characters or less.") String fullName,

        @NotBlank(message = "Enter an email address.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 150, message = "Use 150 characters or less.") String email,

        @NotBlank(message = "Enter a phone number.")
        @Pattern(regexp = "\\+?[0-9][0-9 ()-]{6,19}", message = "Use 7 to 20 characters. Start with a number or +.")
        @Size(max = 20, message = "Use 20 characters or less.") String phone,

        @NotBlank(message = "Enter a department.")
        @Size(max = 60, message = "Use 60 characters or less.") String department,

        @NotBlank(message = "Enter a designation.")
        @Size(max = 80, message = "Use 80 characters or less.") String designation,

        @NotNull(message = "Choose a joining date.") LocalDate dateOfJoining,

        @NotNull(message = "Enter a salary.")
        @DecimalMin(value = "0.00", message = "Salary cannot be below zero.")
        @Digits(integer = 10, fraction = 2, message = "Use up to 10 digits and 2 decimal places.") BigDecimal salary,

        @NotNull(message = "Choose Active or Inactive.") EmploymentStatus status
) {
    public EmployeeRequest {
        employeeId = clean(employeeId);
        if (employeeId != null) employeeId = employeeId.toUpperCase(Locale.ROOT);
        fullName = clean(fullName);
        email = clean(email);
        if (email != null) email = email.toLowerCase(Locale.ROOT);
        phone = clean(phone);
        department = clean(department);
        designation = clean(designation);
    }

    private static String clean(String value) {
        return value == null ? null : value.strip();
    }
}
