package humanit.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.http.HttpStatus;

public final class ApiAssertions {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ApiAssertions() {}

  public static JsonNode parseJson(String body) {
    try {
      return MAPPER.readTree(body);
    } catch (IOException e) {
      throw new AssertionError("Response body is not valid JSON: " + body, e);
    }
  }

  public static JsonNode assertProblem(
      String body, HttpStatus status, String type, String title, String errorCode) {
    JsonNode problem = parseJson(body);

    assertThat(problem.path("status").asInt()).isEqualTo(status.value());
    assertThat(problem.path("type").asText()).isEqualTo(type);
    assertThat(problem.path("title").asText()).isEqualTo(title);
    assertThat(problem.path("errorCode").asText()).isEqualTo(errorCode);

    return problem;
  }

  public static void assertValidationError(JsonNode problem, String field) {
    assertThat(problem.path("errors"))
        .anySatisfy(error -> assertThat(error.path("field").asText()).isEqualTo(field));
  }
}
