package humanit.document.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDocumentRequest(
        @NotBlank @Size(max = 100) String number,
        @Size(max = 500) String description,
        @NotNull LocalDate expirationDate) {
}
