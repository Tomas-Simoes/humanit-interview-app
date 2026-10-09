package humanit.client;

import humanit.client.dto.ClientResponse;
import humanit.client.dto.CreateClientRequest;
import humanit.client.dto.UpdateClientRequest;
import humanit.document.Document;
import humanit.document.dto.DocumentResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {
  public Client toEntity(CreateClientRequest request) {
    Client client =
        new Client(
            request.firstName(),
            request.lastName(),
            request.taxIdentifier(),
            request.email(),
            request.phoneNumber());

    request.documents().stream()
        .map(
            document ->
                new Document(document.number(), document.description(), document.expirationDate()))
        .forEach(client::addDocument);

    return client;
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
        .map(
            document ->
                new DocumentResponse(
                    document.getId(),
                    document.getNumber(),
                    document.getDescription(),
                    document.getExpirationDate(),
                    client.getId()))
        .toList();
  }
}
