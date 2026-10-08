package humanit.client.dto;

import java.util.List;

import humanit.document.dto.DocumentResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Full client representation.")
public record ClientResponse(
        @Schema(description = "Client identifier.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,
        @Schema(description = "Client first name.", example = "Ana")
        String firstName,
        @Schema(description = "Client last name.", example = "Silva")
        String lastName,
        @Schema(description = "Unique tax identifier for the client.", example = "PT123456789")
        String taxIdentifier,
        @Schema(description = "Unique email address for the client.", example = "ana.silva@example.com")
        String email,
        @Schema(description = "Client phone number.", example = "+351912345678")
        String phoneNumber,
        @Schema(description = "Documents belonging to the client.")
        List<DocumentResponse> documents
) {
}
