package com.example.employees.employee;

import com.example.employees.error.DuplicateEmployeeException;
import com.example.employees.error.EmployeeNotFoundException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
    private final EmployeeRepository repository;
    private static final Sort LIST_ORDER = Sort.by("fullName").ascending().and(Sort.by("id"));

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    public PageResponse<EmployeeResponse> list(String search, String department, EmploymentStatus status, int page, int size) {
        return PageResponse.from(repository.findAll(EmployeeFilter.matching(search, department, status),
                PageRequest.of(page, size, LIST_ORDER)).map(EmployeeResponse::from));
    }

    public EmployeeResponse get(Long id) {
        return EmployeeResponse.from(find(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        checkDuplicates(request, -1L);
        return EmployeeResponse.from(repository.saveAndFlush(new Employee(request)));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = find(id);
        checkDuplicates(request, id);
        employee.update(request);
        return EmployeeResponse.from(repository.saveAndFlush(employee));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    public List<String> departments() {
        return repository.findDepartments();
    }

    public EmployeeSummary summary() {
        return new EmployeeSummary(repository.count(), repository.countByStatus(EmploymentStatus.ACTIVE),
                repository.countByStatus(EmploymentStatus.INACTIVE), repository.countDepartments());
    }

    public String exportCsv(String search, String department, EmploymentStatus status) {
        var csv = new StringBuilder("Employee ID,Full Name,Email,Phone,Department,Designation,Date of Joining,Salary,Status\r\n");
        for (Employee employee : repository.findAll(EmployeeFilter.matching(search, department, status), LIST_ORDER)) {
            List<String> row = List.of(employee.getEmployeeId(), employee.getFullName(), employee.getEmail(),
                    employee.getPhone(), employee.getDepartment(), employee.getDesignation(),
                    employee.getDateOfJoining().toString(), employee.getSalary().toPlainString(), employee.getStatus().name());
            csv.append(String.join(",", row.stream().map(EmployeeService::csvCell).toList())).append("\r\n");
        }
        return csv.toString();
    }

    private Employee find(Long id) {
        return repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private void checkDuplicates(EmployeeRequest request, Long currentId) {
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.email(), currentId)) {
            throw new DuplicateEmployeeException("email", "This email is already in use.");
        }
        if (repository.existsByEmployeeIdIgnoreCaseAndIdNot(request.employeeId(), currentId)) {
            throw new DuplicateEmployeeException("employeeId", "This employee ID is already in use.");
        }
    }

    private static String csvCell(String value) {
        if (!value.isEmpty() && "=+-@\t\r\n".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
