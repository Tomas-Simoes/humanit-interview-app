package humanit.client.dto;

public record ClientResponse(
        Long id,
        String firstName,
        String lastName,
        String taxIdentifier,
        String email,
        String phoneNumber
// TODO ,List<DocumentResponse> documents
) {
}
