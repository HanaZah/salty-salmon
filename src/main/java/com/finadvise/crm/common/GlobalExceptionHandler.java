package com.finadvise.crm.common;

import com.finadvise.crm.addresses.AddressValidationException;
import com.finadvise.crm.documents.*;
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
        problem.setType(URI.create(BASE_URL + "concurrent-update"));

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

    @ExceptionHandler(MissingVersionException.class)
    public ResponseEntity<ProblemDetail> handleMissingVersion(MissingVersionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Missing Version");
        problem.setType(URI.create(BASE_URL + "missing-version"));

        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ProblemDetail> handleResourceConflict(ResourceConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Resource Conflict");
        problem.setType(URI.create(BASE_URL + "resource-conflict"));
        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(MalformedDocumentPayloadException.class)
    public ResponseEntity<ProblemDetail> handleMalformedDocumentPayload(MalformedDocumentPayloadException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Malformed Document Payload");
        problem.setType(URI.create(BASE_URL + "malformed-document-payload"));
        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(UnsupportedDocumentFormatException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedDocumentFormat(UnsupportedDocumentFormatException ex) {
        // 415 Unsupported Media Type is the perfect semantic match for bad extensions/MIME types
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
        problem.setTitle("Unsupported Document Format");
        problem.setType(URI.create(BASE_URL + "unsupported-document-format"));
        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(UnreadableDocumentException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableDocument(UnreadableDocumentException ex) {
        // 422 Unprocessable Content tells the client the payload format was fine, but the data itself is unreadable/corrupt
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Unreadable Document");
        problem.setType(URI.create(BASE_URL + "unreadable-document"));
        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(DmsUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleDmsUnavailable() {
        // 503 Service Unavailable is standard for downstream infrastructure failures (like AWS S3)
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The document management system is currently unavailable. Please try again later.");
        problem.setTitle("Storage System Unavailable");
        problem.setType(URI.create(BASE_URL + "dms-unavailable"));
        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(DocumentBatchProcessingException.class)
    public ResponseEntity<ProblemDetail> handleDocumentBatchProcessing(DocumentBatchProcessingException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof ResourceNotFoundException) {
            // Delegate back to the 404 handler
            return handleResourceNotFound((ResourceNotFoundException) cause);
        }

        // 500 Internal Server Error for the rest
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing the document batch.");
        problem.setTitle("Batch Processing Failure");
        problem.setType(URI.create(BASE_URL + "batch-processing-failure"));
        return ResponseEntity.of(problem).build();
    }

    @ExceptionHandler(InvalidInputValueException.class)
    public ResponseEntity<ProblemDetail> handleInvalidInput(InvalidInputValueException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Input Value");
        problem.setType(URI.create(BASE_URL + "invalid-input-value"));
        return ResponseEntity.of(problem).build();
    }
}
