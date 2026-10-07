package humanit.client;

import java.util.List;

import org.springframework.stereotype.Component;

import humanit.client.dto.ClientResponse;
import humanit.client.dto.CreateClientRequest;
import humanit.client.dto.UpdateClientRequest;
import humanit.document.dto.DocumentResponse;

@Component
public class ClientMapper {
    public Client toEntity(CreateClientRequest request) {
        return new Client(
                request.firstName(),
                request.lastName(),
                request.taxIdentifier(),
                request.email(),
                request.phoneNumber());
    }

    public ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getTaxIdentifier(),
                client.getEmail(),
                client.getPhoneNumber(),
                toDocumentResponses(client));
    }

    public void updateEntity(Client client, UpdateClientRequest request) {
        client.updateDetails(
                request.firstName(),
                request.lastName(),
                request.taxIdentifier(),
                request.email(),
                request.phoneNumber());
    }

    private List<DocumentResponse> toDocumentResponses(Client client) {
        return client.getDocuments().stream()
                .map(document -> new DocumentResponse(
                        document.getId(),
                        document.getNumber(),
                        document.getDescription(),
                        document.getExpirationDate(),
                        client.getId()))
                .toList();
    }
}
