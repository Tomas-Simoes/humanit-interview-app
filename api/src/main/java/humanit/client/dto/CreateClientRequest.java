package humanit.client.dto;

import humanit.document.dto.CreateDocumentRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;

@Schema(description = "Client data required to create a client.")
public record CreateClientRequest(
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
        String phoneNumber,
    @Schema(description = "Optional documents to create together with the client.") @Size(max = 50)
        List<@Valid CreateDocumentRequest> documents) {

  public CreateClientRequest(
      String firstName, String lastName, String taxIdentifier, String email, String phoneNumber) {
    this(firstName, lastName, taxIdentifier, email, phoneNumber, List.of());
  }

  public CreateClientRequest {
    firstName = trim(firstName);
    lastName = trim(lastName);
    taxIdentifier = trim(taxIdentifier);
    email = normalizeEmail(email);
    phoneNumber = trim(phoneNumber);
    documents = documents == null ? List.of() : List.copyOf(documents);
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }

  private static String normalizeEmail(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }
}
