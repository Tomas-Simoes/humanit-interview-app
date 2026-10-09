package humanit.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Full document representation.")
public record DocumentResponse(
    @Schema(
            description = "Document identifier.",
            example = "10",
            accessMode = Schema.AccessMode.READ_ONLY)
        Long id,
    @Schema(
            description = "Document number unique within the owning client.",
            example = "DOC-2026-001")
        String number,
    @Schema(description = "Optional document description.", example = "Residence permit")
        String description,
    @Schema(description = "Document expiration date.", example = "2027-12-31")
        LocalDate expirationDate,
    @Schema(
            description = "Owning client identifier.",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
        Long clientId) {}
