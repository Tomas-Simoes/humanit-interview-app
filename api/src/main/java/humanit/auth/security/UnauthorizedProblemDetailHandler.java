package humanit.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import humanit.error.ApiProblemFactory;
import humanit.error.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class UnauthorizedProblemDetailHandler implements AuthenticationEntryPoint {
  private final ObjectMapper objectMapper;

  public UnauthorizedProblemDetailHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    var problem =
        ApiProblemFactory.problem(
            ErrorCode.AUTHENTICATION_REQUIRED, "Missing or invalid bearer token.");

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
