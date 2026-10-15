package com.example.employees.employee;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeFilter {
    private EmployeeFilter() {}

    public static Specification<Employee> matching(String search, String department, EmploymentStatus status) {
        return (root, query, builder) -> {
            var filters = new ArrayList<Predicate>();
            if (search != null && !search.isBlank()) {
                String value = search.strip().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                filters.add(builder.or(
                        builder.like(builder.lower(root.get("fullName")), "%" + value + "%", '\\'),
                        builder.like(builder.lower(root.get("email")), "%" + value + "%", '\\')));
            }
            if (department != null && !department.isBlank()) {
                filters.add(builder.equal(builder.lower(root.get("department")), department.strip().toLowerCase(Locale.ROOT)));
            }
            if (status != null) filters.add(builder.equal(root.get("status"), status));
            return builder.and(filters.toArray(Predicate[]::new));
        };
    }
}
