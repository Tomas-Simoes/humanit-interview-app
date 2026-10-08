package humanit.document.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Document data required to create a document.")
public record CreateDocumentRequest(
        @Schema(description = "Document number unique within the owning client.", example = "DOC-2026-001")
        @NotBlank @Size(max = 100) String number,
        @Schema(description = "Optional document description.", example = "Residence permit")
        @Size(max = 500) String description,
        @Schema(description = "Document expiration date.", example = "2027-12-31")
        @NotNull LocalDate expirationDate) {
}
