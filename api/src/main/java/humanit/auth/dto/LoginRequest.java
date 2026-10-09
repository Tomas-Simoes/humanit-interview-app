package humanit.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials used to request an access token.")
public record LoginRequest(
    @Schema(description = "User email address.", example = "admin@example.com") @NotBlank
        String username,
    @Schema(
            description = "User password.",
            example = "S3curePass!23",
            format = "password",
            accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        String password) {}
