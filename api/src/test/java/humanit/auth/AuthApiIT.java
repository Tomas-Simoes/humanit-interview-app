package humanit.auth;

import static humanit.helper.ApiAssertions.assertProblem;
import static humanit.helper.ApiAssertions.assertValidationError;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.databind.JsonNode;

import humanit.auth.dto.LoginRequest;
import humanit.auth.dto.LoginResponse;
import humanit.auth.dto.RegisterRequest;
import humanit.auth.dto.RegisterResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = "DELETE FROM app_users", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthApiIT {
    @Autowired
    TestRestTemplate rest;

    @Test
    void registerReturns201AndCreatesUser() {
        var response = rest.postForEntity(
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
        var response = rest.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest("bad-email", "password"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode problem = assertProblem(
                response.getBody(),
                HttpStatus.BAD_REQUEST,
                "/problems/validation-failed",
                "Validation failed",
                "VALIDATION_FAILED");
        assertValidationError(problem, "email");
    }

    @Test
    void registerWithDuplicateEmailReturns409() {
        register("ana.user@example.com");

        var response = rest.postForEntity(
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
    void loginAfterRegisterReturnsAccessToken() {
        register("ana.user@example.com");

        var response = rest.postForEntity(
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
        var response = rest.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest("", "password"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode problem = assertProblem(
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

        var response = rest.postForEntity(
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
    }

    private void register(String email) {
        var response = rest.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest(email, "password"),
                RegisterResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
