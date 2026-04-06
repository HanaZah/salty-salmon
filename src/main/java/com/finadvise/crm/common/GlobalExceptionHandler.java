package com.finadvise.crm.common;

import com.finadvise.crm.addresses.AddressValidationException;
import com.finadvise.crm.users.InvalidPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_URL = "https://api.finadvise.com/errors/";

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPassword(InvalidPasswordException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Password");
        problem.setType(URI.create(BASE_URL + "invalid-password"));
        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create(BASE_URL + "not-found"));
        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "The username or password provided is incorrect."
        );
        problem.setTitle("Authentication Failed");
        problem.setType(URI.create(BASE_URL + "authentication-failed"));
        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(AddressValidationException.class)
    public ResponseEntity<ProblemDetail> handleAddressValidation(AddressValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Address Validation Failed");
        problem.setType(URI.create(BASE_URL + "address-validation-failed"));
        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more fields failed validation constraints."
        );
        problem.setTitle("Validation Failed");
        problem.setType(URI.create(BASE_URL + "validation-failed"));

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        problem.setProperty("errors", errors);

        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockingFailure() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The record was modified by another user. " +
                        "Please refresh the page to see the latest data before making your changes."
        );
        problem.setTitle("Concurrent Update Conflict");
        problem.setType(URI.create("https://api.yourdomain.com/errors/concurrent-update"));

        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        problem.setTitle("Access Denied");
        problem.setType(URI.create(BASE_URL + "access-denied"));

        return ResponseEntity.of(problem).build();
    }
}
