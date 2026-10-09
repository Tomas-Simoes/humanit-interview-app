package humanit.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Client summary used in paginated client lists.")
public record ClientSummaryResponse(
    @Schema(
            description = "Client identifier.",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
        Long id,
    @Schema(description = "Client first name.", example = "Ana") String firstName,
    @Schema(description = "Client last name.", example = "Silva") String lastName,
    @Schema(description = "Unique tax identifier for the client.", example = "123456789")
        String taxIdentifier,
    @Schema(description = "Unique email address for the client.", example = "ana.silva@example.com")
        String email,
    @Schema(description = "Client phone number.", example = "+351912345678") String phoneNumber,
    @Schema(description = "Number of documents belonging to the client.", example = "3")
        Long documentCount) {}
