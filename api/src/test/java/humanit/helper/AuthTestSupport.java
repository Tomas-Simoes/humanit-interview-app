package humanit.helper;

import static org.assertj.core.api.Assertions.assertThat;

import humanit.auth.dto.LoginRequest;
import humanit.auth.dto.LoginResponse;
import humanit.auth.dto.RegisterRequest;
import humanit.auth.dto.RegisterResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

public final class AuthTestSupport {
  private static final AtomicLong USER_SEQUENCE = new AtomicLong();
  private static final String PASSWORD = "password";

  private AuthTestSupport() {}

  public static String authenticate(TestRestTemplate rest) {
    rest.getRestTemplate().setInterceptors(List.of());

    String email = "api-test-%d@example.com".formatted(USER_SEQUENCE.incrementAndGet());

    var registerResponse =
        rest.postForEntity(
            "/api/v1/auth/register", new RegisterRequest(email, PASSWORD), RegisterResponse.class);

    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    var loginResponse =
        rest.postForEntity(
            "/api/v1/auth/login", new LoginRequest(email, PASSWORD), LoginResponse.class);

    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResponse.getBody()).isNotNull();

    String accessToken = loginResponse.getBody().accessToken();
    rest.getRestTemplate()
        .setInterceptors(
            List.of(
                (request, body, execution) -> {
                  if (!request.getURI().getPath().startsWith("/api/v1/auth/")) {
                    request.getHeaders().setBearerAuth(accessToken);
                  }
                  return execution.execute(request, body);
                }));

    return accessToken;
  }
}
