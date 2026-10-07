package humanit.document.dto;

import java.time.LocalDate;

public record DocumentSummaryResponse(
        Long id,
        String number,
        String description,
        LocalDate expirationDate,
        Long clientId) {
}
