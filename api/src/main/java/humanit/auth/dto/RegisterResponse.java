package humanit.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered user representation.")
public record RegisterResponse(
    @Schema(
            description = "User identifier.",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
        Long id,
    @Schema(description = "User email address.", example = "admin@example.com") String email,
    @Schema(description = "Assigned user role.", example = "USER") String role) {}
