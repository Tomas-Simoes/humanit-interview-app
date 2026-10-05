package humanit.client.dto;

public record ClientSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String taxIdentifier,
        String email,
        String phoneNumber,
        Long documentCount) {
}
