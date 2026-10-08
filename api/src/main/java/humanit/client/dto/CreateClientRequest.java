package humanit.client.dto;

import java.util.List;

import humanit.document.dto.CreateDocumentRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Client data required to create a client.")
public record CreateClientRequest(
        @Schema(description = "Client first name.", example = "Ana")
        @NotBlank @Size(max = 100) String firstName,
        @Schema(description = "Client last name.", example = "Silva")
        @NotBlank @Size(max = 100) String lastName,
        @Schema(description = "Unique tax identifier for the client.", example = "PT123456789")
        @NotBlank @Size(max = 50) String taxIdentifier,
        @Schema(description = "Unique email address for the client.", example = "ana.silva@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "Client phone number.", example = "+351912345678")
        @NotBlank @Size(max = 30) String phoneNumber,
        @Schema(description = "Optional documents to create together with the client.")
        @Size(max = 50) List<@Valid CreateDocumentRequest> documents) {

    public CreateClientRequest(
            String firstName,
            String lastName,
            String taxIdentifier,
            String email,
            String phoneNumber) {
        this(firstName, lastName, taxIdentifier, email, phoneNumber, List.of());
    }

    public CreateClientRequest {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }
}
