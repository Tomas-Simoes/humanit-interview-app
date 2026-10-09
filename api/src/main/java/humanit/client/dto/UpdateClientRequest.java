package humanit.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

@Schema(description = "Client data required to update a client.")
public record UpdateClientRequest(
    @Schema(description = "Client first name.", example = "Ana") @NotBlank @Size(max = 100)
        String firstName,
    @Schema(description = "Client last name.", example = "Silva") @NotBlank @Size(max = 100)
        String lastName,
    @Schema(description = "Unique Portuguese NIF for the client.", example = "123456789")
        @NotBlank
        @Size(min = 9, max = 9)
        String taxIdentifier,
    @Schema(description = "Unique email address for the client.", example = "ana.silva@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,
    @Schema(description = "Client phone number.", example = "+351912345678")
        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "must be in E.164 format")
        @Size(max = 30)
        String phoneNumber) {

  public UpdateClientRequest {
    firstName = trim(firstName);
    lastName = trim(lastName);
    taxIdentifier = trim(taxIdentifier);
    email = normalizeEmail(email);
    phoneNumber = trim(phoneNumber);
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }

  private static String normalizeEmail(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }
}
