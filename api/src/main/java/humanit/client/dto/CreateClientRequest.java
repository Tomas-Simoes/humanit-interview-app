package humanit.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 50) String taxIdentifier,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 30) String phoneNumber) {
}