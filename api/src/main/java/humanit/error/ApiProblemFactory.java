package humanit.error;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class ApiProblemFactory {
  private static final URI PROBLEM_BASE_URI = URI.create("/problems/");

  private ApiProblemFactory() {}

  public static ResponseEntity<ProblemDetail> response(ErrorCode code, String detail) {
    ProblemSpec spec = problemSpec(code);
    return ResponseEntity.status(spec.status()).body(problem(code, detail));
  }

  public static ProblemDetail problem(ErrorCode code, String detail) {
    ProblemSpec spec = problemSpec(code);

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(spec.status(), detail);
    problem.setType(spec.type());
    problem.setTitle(spec.title());
    problem.setProperty("errorCode", code.name());

    return problem;
  }

  private static ProblemSpec problemSpec(ErrorCode code) {
    return switch (code) {
      case CLIENT_NOT_FOUND ->
          new ProblemSpec(
              HttpStatus.NOT_FOUND,
              PROBLEM_BASE_URI.resolve("client-not-found"),
              "Client not found");
      case DOCUMENT_NOT_FOUND ->
          new ProblemSpec(
              HttpStatus.NOT_FOUND,
              PROBLEM_BASE_URI.resolve("document-not-found"),
              "Document not found");
      case CLIENT_EMAIL_EXISTS ->
          new ProblemSpec(
              HttpStatus.CONFLICT,
              PROBLEM_BASE_URI.resolve("client-email-already-exists"),
              "Client email already exists");
      case CLIENT_TAX_IDENTIFIER_EXISTS ->
          new ProblemSpec(
              HttpStatus.CONFLICT,
              PROBLEM_BASE_URI.resolve("client-tax-identifier-already-exists"),
              "Client tax identifier already exists");
      case DOCUMENT_NUMBER_EXISTS ->
          new ProblemSpec(
              HttpStatus.CONFLICT,
              PROBLEM_BASE_URI.resolve("document-number-already-exists"),
              "Document number already exists");
      case VALIDATION_FAILED ->
          new ProblemSpec(
              HttpStatus.BAD_REQUEST,
              PROBLEM_BASE_URI.resolve("validation-failed"),
              "Validation failed");
      case MALFORMED_REQUEST ->
          new ProblemSpec(
              HttpStatus.BAD_REQUEST,
              PROBLEM_BASE_URI.resolve("malformed-request"),
              "Malformed request");
      case INVALID_SORT ->
          new ProblemSpec(
              HttpStatus.BAD_REQUEST, PROBLEM_BASE_URI.resolve("invalid-sort"), "Invalid sort");
      case DATA_INTEGRITY_VIOLATION ->
          new ProblemSpec(
              HttpStatus.CONFLICT,
              PROBLEM_BASE_URI.resolve("data-integrity-violation"),
              "Data integrity violation");
      case INVALID_CREDENTIALS ->
          new ProblemSpec(
              HttpStatus.UNAUTHORIZED,
              PROBLEM_BASE_URI.resolve("invalid-credentials"),
              "Invalid credentials");
      case AUTHENTICATION_REQUIRED ->
          new ProblemSpec(
              HttpStatus.UNAUTHORIZED,
              PROBLEM_BASE_URI.resolve("authentication-required"),
              "Authentication required");
      case ACCESS_DENIED ->
          new ProblemSpec(
              HttpStatus.FORBIDDEN, PROBLEM_BASE_URI.resolve("access-denied"), "Access denied");
      case METHOD_NOT_ALLOWED ->
          new ProblemSpec(
              HttpStatus.METHOD_NOT_ALLOWED,
              PROBLEM_BASE_URI.resolve("method-not-allowed"),
              "Method not allowed");
      case UNSUPPORTED_MEDIA_TYPE ->
          new ProblemSpec(
              HttpStatus.UNSUPPORTED_MEDIA_TYPE,
              PROBLEM_BASE_URI.resolve("unsupported-media-type"),
              "Unsupported media type");
      case INTERNAL_SERVER_ERROR ->
          new ProblemSpec(
              HttpStatus.INTERNAL_SERVER_ERROR,
              PROBLEM_BASE_URI.resolve("internal-server-error"),
              "Internal server error");
      case USER_EMAIL_EXISTS ->
          new ProblemSpec(
              HttpStatus.CONFLICT,
              PROBLEM_BASE_URI.resolve("user-email-already-exists"),
              "User email already exists");
    };
  }

  private record ProblemSpec(HttpStatus status, URI type, String title) {}
}
