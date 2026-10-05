package humanit.client;

import org.springframework.stereotype.Component;

import humanit.client.dto.ClientResponse;
import humanit.client.dto.CreateClientRequest;

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
                client.getPhoneNumber()
        // TODO ,client.getDocuments()
        );
    }
}
