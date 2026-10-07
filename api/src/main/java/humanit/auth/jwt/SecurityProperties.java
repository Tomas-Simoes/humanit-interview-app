package humanit.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
                @Valid Jwt jwt) {
        public record Jwt(
                        @NotBlank String issuer,
                        @NotBlank String audience,
                        @Positive long expirationSeconds,
                        @NotBlank String secret) {
        }
}
