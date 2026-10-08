package humanit.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Client data required to update a client.")
public record UpdateClientRequest(
        @Schema(description = "Client first name.", example = "Ana")
        @NotBlank @Size(max = 100) String firstName,
        @Schema(description = "Client last name.", example = "Silva")
        @NotBlank @Size(max = 100) String lastName,
        @Schema(description = "Unique tax identifier for the client.", example = "PT123456789")
        @NotBlank @Size(max = 50) String taxIdentifier,
        @Schema(description = "Unique email address for the client.", example = "ana.silva@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "Client phone number.", example = "+351912345678")
        @NotBlank @Size(max = 30) String phoneNumber) {
}
