package humanit.error;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final URI PROBLEM_BASE_URI = URI.create("/problems/");

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ProblemDetail> handleApplicationException(ApplicationException e) {
        return problemResponse(e.errorCode(), e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return problemResponse(
                ErrorCode.DATA_INTEGRITY_VIOLATION,
                "Request conflicts with existing data.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        ProblemDetail problem = problem(
                ErrorCode.VALIDATION_FAILED,
                "Request validation failed.");

        List<ValidationError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();

        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return problemResponse(
                ErrorCode.MALFORMED_REQUEST,
                "Request body is missing or malformed.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        return problemResponse(
                ErrorCode.MALFORMED_REQUEST,
                "Request parameter or path variable has an invalid value.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException e) {
        ProblemDetail problem = problem(
                ErrorCode.VALIDATION_FAILED,
                "Request validation failed.");

        List<ValidationError> errors = e.getConstraintViolations()
                .stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(violation -> new ValidationError(fieldName(violation), violation.getMessage()))
                .toList();

        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    private ResponseEntity<ProblemDetail> problemResponse(ErrorCode code, String detail) {
        ProblemSpec spec = problemSpec(code);
        return ResponseEntity.status(spec.status()).body(problem(code, detail));
    }

    private ProblemDetail problem(ErrorCode code, String detail) {
        ProblemSpec spec = problemSpec(code);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(spec.status(), detail);
        problem.setType(spec.type());
        problem.setTitle(spec.title());
        problem.setProperty("errorCode", code.name());

        return problem;
    }

    private ProblemSpec problemSpec(ErrorCode code) {
        return switch (code) {
            case CLIENT_NOT_FOUND -> new ProblemSpec(
                    HttpStatus.NOT_FOUND,
                    PROBLEM_BASE_URI.resolve("client-not-found"),
                    "Client not found");
            case DOCUMENT_NOT_FOUND -> new ProblemSpec(
                    HttpStatus.NOT_FOUND,
                    PROBLEM_BASE_URI.resolve("document-not-found"),
                    "Document not found");
            case CLIENT_EMAIL_EXISTS -> new ProblemSpec(
                    HttpStatus.CONFLICT,
                    PROBLEM_BASE_URI.resolve("client-email-already-exists"),
                    "Client email already exists");
            case CLIENT_TAX_IDENTIFIER_EXISTS -> new ProblemSpec(
                    HttpStatus.CONFLICT,
                    PROBLEM_BASE_URI.resolve("client-tax-identifier-already-exists"),
                    "Client tax identifier already exists");
            case DOCUMENT_NUMBER_EXISTS -> new ProblemSpec(
                    HttpStatus.CONFLICT,
                    PROBLEM_BASE_URI.resolve("document-number-already-exists"),
                    "Document number already exists");
            case VALIDATION_FAILED -> new ProblemSpec(
                    HttpStatus.BAD_REQUEST,
                    PROBLEM_BASE_URI.resolve("validation-failed"),
                    "Validation failed");
            case MALFORMED_REQUEST -> new ProblemSpec(
                    HttpStatus.BAD_REQUEST,
                    PROBLEM_BASE_URI.resolve("malformed-request"),
                    "Malformed request");
            case DATA_INTEGRITY_VIOLATION -> new ProblemSpec(
                    HttpStatus.CONFLICT,
                    PROBLEM_BASE_URI.resolve("data-integrity-violation"),
                    "Data integrity violation");
            case INVALID_CREDENTIALS -> new ProblemSpec(
                    HttpStatus.UNAUTHORIZED,
                    PROBLEM_BASE_URI.resolve("invalid-credentials"),
                    "Invalid credentials");
            case INTERNAL_SERVER_ERROR -> new ProblemSpec(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    PROBLEM_BASE_URI.resolve("internal-server-error"),
                    "Internal server error");
            case USER_EMAIL_EXISTS -> new ProblemSpec(
                    HttpStatus.CONFLICT,
                    PROBLEM_BASE_URI.resolve("user-email-already-exists"),
                    "User email already exists");
        };
    }

    private String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastSeparator = path.lastIndexOf('.');
        return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
    }

    private record ProblemSpec(HttpStatus status, URI type, String title) {
    }

    private record ValidationError(String field, String message) {
    }
}
