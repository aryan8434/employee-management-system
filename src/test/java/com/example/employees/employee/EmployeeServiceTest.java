package com.example.employees.employee;

import com.example.employees.error.DuplicateEmployeeException;
import com.example.employees.error.EmployeeNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    @Mock private EmployeeRepository repository;
    private EmployeeService service;

    @BeforeEach
    void setUp() { service = new EmployeeService(repository); }

    private EmployeeRequest request() {
        return new EmployeeRequest(" emp-101 ", " Ananya Rao ", " Ananya@Example.com ",
                "+91 98765 43210", "Engineering", "Backend Developer", LocalDate.of(2025, 8, 12),
                new BigDecimal("600000.00"), EmploymentStatus.ACTIVE);
    }

    private Employee employee() {
        Employee employee = new Employee(request());
        ReflectionTestUtils.setField(employee, "id", 1L);
        return employee;
    }

    @Test
    void createSavesCleanValues() {
        when(repository.saveAndFlush(any(Employee.class))).thenAnswer(call -> call.getArgument(0));
        EmployeeResponse saved = service.create(request());
        assertThat(saved.employeeId()).isEqualTo("EMP-101");
        assertThat(saved.email()).isEqualTo("ananya@example.com");
        assertThat(saved.fullName()).isEqualTo("Ananya Rao");
        assertThat(saved.salary()).isEqualByComparingTo("600000.00");
        verify(repository).saveAndFlush(any(Employee.class));
    }

    @Test
    void createRejectsAnEmailThatIsAlreadyUsed() {
        when(repository.existsByEmailIgnoreCaseAndIdNot("ananya@example.com", -1L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DuplicateEmployeeException.class).hasMessage("This email is already in use.");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsAnEmployeeIdThatIsAlreadyUsed() {
        when(repository.existsByEmployeeIdIgnoreCaseAndIdNot("EMP-101", -1L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DuplicateEmployeeException.class).hasMessage("This employee ID is already in use.");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void getReturnsAllEmployeeFields() {
        when(repository.findById(1L)).thenReturn(Optional.of(employee()));
        EmployeeResponse result = service.get(1L);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.department()).isEqualTo("Engineering");
        assertThat(result.phone()).isEqualTo("+91 98765 43210");
        assertThat(result.status()).isEqualTo(EmploymentStatus.ACTIVE);
        assertThat(result.dateOfJoining()).isEqualTo(LocalDate.of(2025, 8, 12));
    }

    @Test
    void getReportsAMissingEmployee() {
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void updateKeepsTheRecordIdAndAllowsItsOwnEmail() {
        Employee existing = employee();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);
        EmployeeRequest changed = new EmployeeRequest("EMP-101", "Ananya Rao", "ananya@example.com",
                "9876543210", "Product", "Product Engineer", LocalDate.of(2025, 8, 12),
                new BigDecimal("700000"), EmploymentStatus.INACTIVE);
        EmployeeResponse result = service.update(1L, changed);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.department()).isEqualTo("Product");
        assertThat(result.status()).isEqualTo(EmploymentStatus.INACTIVE);
        verify(repository).existsByEmailIgnoreCaseAndIdNot("ananya@example.com", 1L);
    }

    @Test
    void updateRejectsAnEmailOwnedBySomeoneElse() {
        Employee existing = employee();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailIgnoreCaseAndIdNot(anyString(), eq(1L))).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, request())).isInstanceOf(DuplicateEmployeeException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateDoesNotCreateAMissingEmployee() {
        assertThatThrownBy(() -> service.update(99L, request())).isInstanceOf(EmployeeNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deleteRemovesTheFoundRecord() {
        Employee employee = employee();
        when(repository.findById(1L)).thenReturn(Optional.of(employee));
        service.delete(1L);
        verify(repository).delete(employee);
    }

    @Test
    void deleteReportsAMissingEmployee() {
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(EmployeeNotFoundException.class);
        verify(repository, never()).delete(any(Employee.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsesTheRequestedPageAndReturnsPageCounts() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(employee()), call.getArgument(1), 12));
        PageResponse<EmployeeResponse> result = service.list("Rao", "Engineering", EmploymentStatus.ACTIVE, 1, 5);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), page.capture());
        assertThat(page.getValue().getPageNumber()).isEqualTo(1);
        assertThat(page.getValue().getPageSize()).isEqualTo(5);
        assertThat(page.getValue().getSort().getOrderFor("id")).isNotNull();
        assertThat(result.totalElements()).isEqualTo(12);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void summaryReturnsCountsForTheWholeTeam() {
        when(repository.count()).thenReturn(12L);
        when(repository.countByStatus(EmploymentStatus.ACTIVE)).thenReturn(9L);
        when(repository.countByStatus(EmploymentStatus.INACTIVE)).thenReturn(3L);
        when(repository.countDepartments()).thenReturn(4L);
        assertThat(service.summary()).isEqualTo(new EmployeeSummary(12, 9, 3, 4));
    }

    @Test
    void departmentsReturnsTheStoredDepartmentList() {
        when(repository.findDepartments()).thenReturn(List.of("Engineering", "Product"));
        assertThat(service.departments()).containsExactly("Engineering", "Product");
    }

    @Test
    @SuppressWarnings("unchecked")
    void csvEscapesQuotesAndProtectsSpreadsheetFormulas() {
        EmployeeRequest request = new EmployeeRequest("EMP-102", "=SUM(1,2)", "test@example.com",
                "+91 98765 43210", "Research, India", "Engineer \"II\"", LocalDate.of(2025, 8, 12),
                new BigDecimal("600000.50"), EmploymentStatus.ACTIVE);
        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(new Employee(request)));
        String csv = service.exportCsv(null, null, null);
        assertThat(csv).startsWith("Employee ID,Full Name,Email,Phone,");
        assertThat(csv).contains("\"'=SUM(1,2)\"", "\"Research, India\"", "\"Engineer \"\"II\"\"\"", "\"'+91 98765 43210\"");
        assertThat(csv).endsWith("\r\n");
    }
}
