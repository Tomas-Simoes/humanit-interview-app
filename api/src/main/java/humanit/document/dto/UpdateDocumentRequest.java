package humanit.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Locale;

@Schema(description = "Document data required to update a document.")
public record UpdateDocumentRequest(
    @Schema(
            description = "Document number unique within the owning client.",
            example = "DOC-2026-001")
        @NotBlank
        @Size(max = 100)
        String number,
    @Schema(description = "Optional document description.", example = "Residence permit")
        @Size(max = 500)
        String description,
    @Schema(description = "Document expiration date.", example = "2027-12-31") @NotNull
        LocalDate expirationDate) {

  public UpdateDocumentRequest {
    number = normalizeDocumentNumber(number);
    description = trim(description);
  }

  private static String normalizeDocumentNumber(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }
}
