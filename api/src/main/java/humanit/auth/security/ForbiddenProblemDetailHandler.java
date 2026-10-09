package humanit.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import humanit.error.ApiProblemFactory;
import humanit.error.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ForbiddenProblemDetailHandler implements AccessDeniedHandler {
  private final ObjectMapper objectMapper;

  public ForbiddenProblemDetailHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    var problem =
        ApiProblemFactory.problem(
            ErrorCode.ACCESS_DENIED, "You do not have permission to access this resource.");

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
