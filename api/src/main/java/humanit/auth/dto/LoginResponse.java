package humanit.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access token response.")
public record LoginResponse(
    @Schema(description = "JWT access token.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
    @Schema(description = "Token type used in the Authorization header.", example = "Bearer")
        String tokenType,
    @Schema(description = "Token lifetime in seconds.", example = "900") long expiresIn) {

  public static LoginResponse bearer(String token, long expiresIn) {
    return new LoginResponse(token, "Bearer", expiresIn);
  }
}
