package humanit.error;

import humanit.auth.jwt.JwtSecretConfigurationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ProblemDetail> handleApplicationException(ApplicationException e) {
    return ApiProblemFactory.response(e.errorCode(), e.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
      DataIntegrityViolationException e) {
    return ApiProblemFactory.response(
        ErrorCode.DATA_INTEGRITY_VIOLATION, "Request conflicts with existing data.");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e) {
    ProblemDetail problem =
        ApiProblemFactory.problem(ErrorCode.VALIDATION_FAILED, "Request validation failed.");

    List<ValidationError> errors =
        e.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
            .toList();

    problem.setProperty("errors", errors);

    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
      HttpMessageNotReadableException e) {
    return ApiProblemFactory.response(
        ErrorCode.MALFORMED_REQUEST, "Request body is missing or malformed.");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException e) {
    return ApiProblemFactory.response(
        ErrorCode.MALFORMED_REQUEST, "Request parameter or path variable has an invalid value.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException e) {
    return ApiProblemFactory.response(
        ErrorCode.METHOD_NOT_ALLOWED, "HTTP method is not supported for this resource.");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException e) {
    return ApiProblemFactory.response(
        ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Request media type is not supported.");
  }

  @ExceptionHandler(JwtSecretConfigurationException.class)
  public ResponseEntity<ProblemDetail> handleInvalidJwtSecret(JwtSecretConfigurationException e) {
    log.error("Invalid JWT secret configuration: {}", e.getMessage());
    return ApiProblemFactory.response(
        ErrorCode.INTERNAL_SERVER_ERROR, "Application security configuration is invalid.");
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException e) {
    ProblemDetail problem =
        ApiProblemFactory.problem(ErrorCode.VALIDATION_FAILED, "Request validation failed.");

    List<ValidationError> errors =
        e.getConstraintViolations().stream()
            .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
            .map(violation -> new ValidationError(fieldName(violation), violation.getMessage()))
            .toList();

    problem.setProperty("errors", errors);

    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
    log.error("Unhandled application error", e);
    return ApiProblemFactory.response(ErrorCode.INTERNAL_SERVER_ERROR, "Unexpected server error.");
  }

  private String fieldName(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    int lastSeparator = path.lastIndexOf('.');
    return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
  }

  private record ValidationError(String field, String message) {}
}
