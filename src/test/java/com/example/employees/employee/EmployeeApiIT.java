package com.example.employees.employee;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeApiIT {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EmployeeRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clearTestDatabase() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).endsWith("_tests");
        repository.deleteAll();
    }

    private EmployeeRequest request(String code, String name, String email, String department, EmploymentStatus status) {
        return new EmployeeRequest(code, name, email, "9876543210", department, "Engineer",
                LocalDate.of(2025, 8, 12), new BigDecimal("650000.50"), status);
    }

    private JsonNode create(EmployeeRequest request) throws Exception {
        String body = mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    @Test
    void fullCrudStoresAndUpdatesEveryField() throws Exception {
        JsonNode saved = create(request("EMP-101", "Ananya Rao", "ananya@example.com", "Engineering", EmploymentStatus.ACTIVE));
        long id = saved.get("id").asLong();
        mvc.perform(get("/api/employees/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(650000.50));
        EmployeeRequest update = new EmployeeRequest("EMP-109", "Ananya R", "new@example.com", "+91 99999 99999",
                "Product", "Lead Engineer", LocalDate.of(2024, 4, 15), new BigDecimal("900000.25"), EmploymentStatus.INACTIVE);
        mvc.perform(put("/api/employees/" + id).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        Employee stored = repository.findById(id).orElseThrow();
        assertThat(EmployeeResponse.from(stored)).isEqualTo(new EmployeeResponse(id, "EMP-109", "Ananya R",
                "new@example.com", "+91 99999 99999", "Product", "Lead Engineer", LocalDate.of(2024, 4, 15),
                new BigDecimal("900000.25"), EmploymentStatus.INACTIVE));
        mvc.perform(delete("/api/employees/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/employees/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateEmailAndEmployeeIdIgnoreCase() throws Exception {
        create(request("EMP-101", "Ananya Rao", "ananya@example.com", "Engineering", EmploymentStatus.ACTIVE));
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                        request("EMP-102", "Amit Shah", "ANANYA@EXAMPLE.COM", "Product", EmploymentStatus.ACTIVE))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errors.email").exists());
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                        request("emp-101", "Amit Shah", "amit@example.com", "Product", EmploymentStatus.ACTIVE))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errors.employeeId").exists());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void databaseAlsoBlocksDuplicateEmailsWithoutTheService() {
        repository.saveAndFlush(new Employee(request("EMP-101", "One", "one@example.com", "Engineering", EmploymentStatus.ACTIVE)));
        Employee other = new Employee(request("EMP-102", "Two", "two@example.com", "Product", EmploymentStatus.ACTIVE));
        org.springframework.test.util.ReflectionTestUtils.setField(other, "email", "ONE@EXAMPLE.COM");
        assertThatThrownBy(() -> repository.saveAndFlush(other)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void searchMatchesNameOrEmailAndCombinesBothFilters() throws Exception {
        create(request("EMP-101", "Ananya Rao", "ananya@example.com", "Engineering", EmploymentStatus.ACTIVE));
        create(request("EMP-102", "Amit Rao", "amit@example.com", "Engineering", EmploymentStatus.INACTIVE));
        create(request("EMP-103", "Nisha Jain", "nisha@example.com", "Product", EmploymentStatus.ACTIVE));
        mvc.perform(get("/api/employees").param("search", "RAO").param("department", "engineering").param("status", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeId").value("EMP-101"));
        mvc.perform(get("/api/employees").param("search", "NISHA@"))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].employeeId").value("EMP-103"));
    }

    @Test
    void percentAndUnderscoreAreLiteralSearchText() throws Exception {
        create(request("EMP-101", "A_100% Rao", "ananya@example.com", "Engineering", EmploymentStatus.ACTIVE));
        create(request("EMP-102", "Ananya Rao", "other@example.com", "Product", EmploymentStatus.ACTIVE));
        mvc.perform(get("/api/employees").param("search", "_100%"))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].employeeId").value("EMP-101"));
    }

    @Test
    void paginationUsesAStableOrderForEqualNames() throws Exception {
        for (int i = 1; i <= 12; i++) create(request("EMP-" + i, "Same Name", "person" + i + "@example.com", "Engineering", EmploymentStatus.ACTIVE));
        var ids = new HashSet<Long>();
        for (int page = 0; page < 3; page++) {
            JsonNode result = mapper.readTree(mvc.perform(get("/api/employees").param("size", "5").param("page", String.valueOf(page)))
                    .andExpect(jsonPath("$.totalElements").value(12)).andExpect(jsonPath("$.totalPages").value(3))
                    .andReturn().getResponse().getContentAsString());
            for (JsonNode employee : result.get("content")) assertThat(ids.add(employee.get("id").asLong())).isTrue();
        }
        assertThat(ids).hasSize(12);
        mvc.perform(get("/api/employees?page=50&size=5")).andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void summaryAndDepartmentListUseTheDatabase() throws Exception {
        create(request("EMP-101", "One", "one@example.com", "Engineering", EmploymentStatus.ACTIVE));
        create(request("EMP-102", "Two", "two@example.com", "engineering", EmploymentStatus.INACTIVE));
        create(request("EMP-103", "Three", "three@example.com", "Product", EmploymentStatus.ACTIVE));
        mvc.perform(get("/api/employees/summary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.active").value(2))
                .andExpect(jsonPath("$.inactive").value(1)).andExpect(jsonPath("$.departments").value(2));
        mvc.perform(get("/api/employees/departments")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void csvExportsAllFilteredRowsAndOpenApiIsAvailable() throws Exception {
        for (int i = 1; i <= 12; i++) create(request("EMP-" + i, "Person " + i, "person" + i + "@example.com", "Engineering", EmploymentStatus.ACTIVE));
        create(request("EMP-99", "Other Person", "other@example.com", "Product", EmploymentStatus.INACTIVE));
        String csv = mvc.perform(get("/api/employees/export?department=Engineering&status=ACTIVE"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(csv.lines().count()).isEqualTo(13);
        assertThat(csv).doesNotContain("EMP-99");
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/employees']").exists());
    }
}
