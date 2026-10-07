package humanit.client.dto;

import java.util.List;

import humanit.document.dto.DocumentResponse;

public record ClientResponse(
        Long id,
        String firstName,
        String lastName,
        String taxIdentifier,
        String email,
        String phoneNumber,
        List<DocumentResponse> documents
) {
}
