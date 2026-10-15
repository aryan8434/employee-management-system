package com.example.employees.employee;

import com.example.employees.error.ApiExceptionHandler;
import com.example.employees.error.EmployeeNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@Import(ApiExceptionHandler.class)
class EmployeeControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private EmployeeService service;

    private String validJson() {
        return """
                {"employeeId":"EMP-101","fullName":"Ananya Rao","email":"ananya@example.com",
                "phone":"9876543210","department":"Engineering","designation":"Backend Developer",
                "dateOfJoining":"2025-08-12","salary":600000,"status":"ACTIVE"}
                """;
    }

    @Test
    void postReturnsCreatedAndLocation() throws Exception {
        when(service.create(any())).thenReturn(new EmployeeResponse(1L, "EMP-101", "Ananya Rao",
                "ananya@example.com", "9876543210", "Engineering", "Backend Developer",
                LocalDate.of(2025, 8, 12), new BigDecimal("600000"), EmploymentStatus.ACTIVE));
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/employees/1"))
                .andExpect(jsonPath("$.fullName").value("Ananya Rao"));
    }

    @Test
    void blankBodyFieldsReturnHelpfulErrors() throws Exception {
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.salary").exists());
        verifyNoInteractions(service);
    }

    @Test
    void invalidEmailAndNegativeSalaryAreRejected() throws Exception {
        String body = validJson().replace("ananya@example.com", "bad-email").replace("600000", "-1");
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.salary").exists());
        verifyNoInteractions(service);
    }

    @Test
    void malformedJsonIsRejected() throws Exception {
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongStatusAndInvalidDateAreRejected() throws Exception {
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("ACTIVE", "PAUSED")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("2025-08-12", "2025-02-30")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidPageAndSizeAreRejected() throws Exception {
        mvc.perform(get("/api/employees?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/employees?size=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/employees?size=101")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void wrongStatusFilterIsRejected() throws Exception {
        mvc.perform(get("/api/employees?status=OTHER")).andExpect(status().isBadRequest());
    }

    @Test
    void missingEmployeeReturnsNotFound() throws Exception {
        when(service.get(99L)).thenThrow(new EmployeeNotFoundException(99L));
        mvc.perform(get("/api/employees/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee 99 was not found."));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/employees/1")).andExpect(status().isNoContent());
        verify(service).delete(1L);
    }

    @Test
    void exportSetsAFileNameAndCsvType() throws Exception {
        when(service.exportCsv(null, "Engineering", EmploymentStatus.ACTIVE)).thenReturn("Employee ID\r\n");
        mvc.perform(get("/api/employees/export?department=Engineering&status=ACTIVE"))
                .andExpect(status().isOk()).andExpect(header().string("Content-Disposition", "attachment; filename=employees.csv"))
                .andExpect(content().contentType("text/csv;charset=UTF-8"));
    }

    @Test
    void anUnknownApiPathReturnsNotFound() throws Exception {
        mvc.perform(get("/api/unknown")).andExpect(status().isNotFound());
    }

    @Test
    void anUnsupportedMethodKeepsItsCorrectStatus() throws Exception {
        mvc.perform(patch("/api/employees/1").contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isMethodNotAllowed());
    }
}
