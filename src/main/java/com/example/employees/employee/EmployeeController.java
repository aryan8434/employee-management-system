package com.example.employees.employee;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees")
public class EmployeeController {
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Find employees", description = "Search name or email. Page starts at 0; size is 1 to 100.")
    public PageResponse<EmployeeResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return service.list(search, department, status, page, size);
    }

    @GetMapping("/departments")
    @Operation(summary = "Get departments used by employees")
    public List<String> departments() { return service.departments(); }

    @GetMapping("/summary")
    @Operation(summary = "Get counts for all employees")
    public EmployeeSummary summary() { return service.summary(); }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export all employees that match the filters")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmploymentStatus status) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(service.exportCsv(search, department, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one employee by the numeric record ID")
    public EmployeeResponse get(@PathVariable @Positive Long id) { return service.get(id); }

    @PostMapping
    @Operation(summary = "Add an employee")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse employee = service.create(request);
        return ResponseEntity.created(URI.create("/api/employees/" + employee.id())).body(employee);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update all fields of an employee")
    public EmployeeResponse update(@PathVariable @Positive Long id, @Valid @RequestBody EmployeeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
