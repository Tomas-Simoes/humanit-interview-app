package humanit.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

@Schema(description = "Data required to register a user.")
public record RegisterRequest(
    @Schema(description = "Unique user email address.", example = "admin@example.com")
        @NotBlank
        @Email
        String email,
    @Schema(
            description = "User password.",
            example = "S3curePass!23",
            format = "password",
            accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        @Size(min = 8, max = 64)
        String password) {

  public RegisterRequest {
    email = normalizeEmail(email);
  }

  private static String normalizeEmail(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }
}
