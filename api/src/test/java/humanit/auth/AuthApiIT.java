package humanit.auth;

import static humanit.helper.ApiAssertions.assertProblem;
import static humanit.helper.ApiAssertions.assertValidationError;
import static humanit.helper.ApiAssertions.parseJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import humanit.auth.dto.LoginRequest;
import humanit.auth.dto.LoginResponse;
import humanit.auth.dto.RegisterRequest;
import humanit.auth.dto.RegisterResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = "DELETE FROM app_users", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthApiIT {
  @Autowired TestRestTemplate rest;

  @Test
  void registerReturns201AndCreatesUser() {
    var response =
        rest.postForEntity(
            "/api/v1/auth/register",
            new RegisterRequest("ana.user@example.com", "password"),
            RegisterResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isNotNull();
    assertThat(response.getBody().email()).isEqualTo("ana.user@example.com");
    assertThat(response.getBody().role()).isEqualTo("USER");
  }

  @Test
  void registerWithInvalidEmailReturns400() {
    var response =
        rest.postForEntity(
            "/api/v1/auth/register", new RegisterRequest("bad-email", "password"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode problem =
        assertProblem(
            response.getBody(),
            HttpStatus.BAD_REQUEST,
            "/problems/validation-failed",
            "Validation failed",
            "VALIDATION_FAILED");
    assertValidationError(problem, "email");
  }

  @Test
  void registerWithTooLongPasswordReturns400() {
    var response =
        rest.postForEntity(
            "/api/v1/auth/register",
            new RegisterRequest("ana.user@example.com", "p".repeat(65)),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode problem =
        assertProblem(
            response.getBody(),
            HttpStatus.BAD_REQUEST,
            "/problems/validation-failed",
            "Validation failed",
            "VALIDATION_FAILED");
    assertValidationError(problem, "password");
  }

  @Test
  void registerWithDuplicateEmailReturns409() {
    register("ana.user@example.com");

    var response =
        rest.postForEntity(
            "/api/v1/auth/register",
            new RegisterRequest("ana.user@example.com", "password"),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertProblem(
        response.getBody(),
        HttpStatus.CONFLICT,
        "/problems/user-email-already-exists",
        "User email already exists",
        "USER_EMAIL_EXISTS");
  }

  @Test
  void registerNormalizesEmailForDuplicateChecks() {
    var created =
        rest.postForEntity(
            "/api/v1/auth/register",
            new RegisterRequest(" ANA.USER@EXAMPLE.COM ", "password"),
            RegisterResponse.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    assertThat(created.getBody().email()).isEqualTo("ana.user@example.com");

    var duplicate =
        rest.postForEntity(
            "/api/v1/auth/register",
            new RegisterRequest("ana.user@example.com", "password"),
            String.class);

    assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertProblem(
        duplicate.getBody(),
        HttpStatus.CONFLICT,
        "/problems/user-email-already-exists",
        "User email already exists",
        "USER_EMAIL_EXISTS");
  }

  @Test
  void loginAfterRegisterReturnsAccessToken() {
    register("ana.user@example.com");

    var response =
        rest.postForEntity(
            "/api/v1/auth/login",
            new LoginRequest("ana.user@example.com", "password"),
            LoginResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().accessToken()).isNotBlank();
    assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
    assertThat(response.getBody().expiresIn()).isEqualTo(900);
  }

  @Test
  void loginWithBlankUsernameReturns400() {
    var response =
        rest.postForEntity("/api/v1/auth/login", new LoginRequest("", "password"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode problem =
        assertProblem(
            response.getBody(),
            HttpStatus.BAD_REQUEST,
            "/problems/validation-failed",
            "Validation failed",
            "VALIDATION_FAILED");
    assertValidationError(problem, "username");
  }

  @Test
  void loginWithInvalidCredentialsReturns401() {
    register("ana.user@example.com");

    var response =
        rest.postForEntity(
            "/api/v1/auth/login",
            new LoginRequest("ana.user@example.com", "wrong-password"),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertProblem(
        response.getBody(),
        HttpStatus.UNAUTHORIZED,
        "/problems/invalid-credentials",
        "Invalid credentials",
        "INVALID_CREDENTIALS");
  }

  @Test
  void clientApiWithoutTokenReturns401() {
    var response = rest.getForEntity("/api/v1/clients", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertProblem(
        response.getBody(),
        HttpStatus.UNAUTHORIZED,
        "/problems/authentication-required",
        "Authentication required",
        "AUTHENTICATION_REQUIRED");
    assertThat(response.getHeaders().getFirst("X-Request-Id")).isNotBlank();
  }

  @Test
  void documentApiWithoutTokenReturns401() {
    var response = rest.getForEntity("/api/v1/documents", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertProblem(
        response.getBody(),
        HttpStatus.UNAUTHORIZED,
        "/problems/authentication-required",
        "Authentication required",
        "AUTHENTICATION_REQUIRED");
  }

  @Test
  void healthEndpointIsPublic() {
    var response = rest.getForEntity("/actuator/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(parseJson(response.getBody()).path("status").asText()).isEqualTo("UP");
  }

  @Test
  void clientApiWithInvalidTokenReturns401Problem() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("not-a-jwt");

    var response =
        rest.exchange("/api/v1/clients", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertProblem(
        response.getBody(),
        HttpStatus.UNAUTHORIZED,
        "/problems/authentication-required",
        "Authentication required",
        "AUTHENTICATION_REQUIRED");
  }

  private void register(String email) {
    var response =
        rest.postForEntity(
            "/api/v1/auth/register",
            new RegisterRequest(email, "password"),
            RegisterResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }
}
