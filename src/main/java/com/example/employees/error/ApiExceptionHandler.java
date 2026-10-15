package com.example.employees.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(EmployeeNotFoundException.class)
    ResponseEntity<ApiError> notFound(EmployeeNotFoundException error) {
        return response(HttpStatus.NOT_FOUND, error.getMessage(), Map.of());
    }

    @ExceptionHandler(DuplicateEmployeeException.class)
    ResponseEntity<ApiError> duplicate(DuplicateEmployeeException error) {
        return response(HttpStatus.CONFLICT, error.getMessage(), Map.of(error.getField(), error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidFields(MethodArgumentNotValidException error) {
        var fields = new LinkedHashMap<String, String>();
        error.getBindingResult().getFieldErrors().forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Check the marked fields.", fields);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class})
    ResponseEntity<ApiError> invalidParameter(Exception error) {
        return response(HttpStatus.BAD_REQUEST, "Check the record ID, page, size and status. Page starts at 0 and size is 1 to 100.", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> invalidBody() {
        return response(HttpStatus.BAD_REQUEST, "Check the request. Use a date like 2026-01-15 and status ACTIVE or INACTIVE.", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> databaseConflict() {
        return response(HttpStatus.CONFLICT, "This email or employee ID is already in use. Please choose another.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception error) {
        if (error instanceof ErrorResponse requestError) {
            String message = requestError.getBody().getDetail();
            return ResponseEntity.status(requestError.getStatusCode()).headers(requestError.getHeaders())
                    .body(new ApiError(requestError.getStatusCode().value(),
                            message == null ? "Check the request." : message, Map.of(), Instant.now()));
        }
        log.error("Could not complete employee request", error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatusCode status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message, errors, Instant.now()));
    }
}
