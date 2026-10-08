package humanit.client;

import java.time.LocalDate;
import java.util.List;

import humanit.client.dto.ClientResponse;
import humanit.client.dto.CreateClientRequest;
import humanit.client.dto.UpdateClientRequest;
import humanit.document.Document;
import humanit.document.dto.CreateDocumentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ClientMapperTest {
    private final ClientMapper mapper = new ClientMapper();

    @Test
    void toEntity_mapsCreateClientRequestToClient() {
        var request = new CreateClientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000");

        Client client = mapper.toEntity(request);

        assertThat(client.getFirstName()).isEqualTo("Ana");
        assertThat(client.getLastName()).isEqualTo("Silva");
        assertThat(client.getTaxIdentifier()).isEqualTo("TAX-1");
        assertThat(client.getEmail()).isEqualTo("ana@example.com");
        assertThat(client.getPhoneNumber()).isEqualTo("910000000");
        assertThat(client.getDocuments()).isEmpty();
    }

    @Test
    void toEntity_mapsCreateClientRequestDocumentsToClientDocuments() {
        var request = new CreateClientRequest(
                "Ana",
                "Silva",
                "TAX-1",
                "ana@example.com",
                "910000000",
                List.of(new CreateDocumentRequest("DOC-1", "Passport", LocalDate.of(2030, 1, 1))));

        Client client = mapper.toEntity(request);

        assertThat(client.getDocuments()).hasSize(1);
        Document document = client.getDocuments().getFirst();
        assertThat(document.getNumber()).isEqualTo("DOC-1");
        assertThat(document.getDescription()).isEqualTo("Passport");
        assertThat(document.getExpirationDate()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(document.getClient()).isSameAs(client);
    }

    @Test
    void toResponse_mapsClientToClientResponse() {
        Client client = new Client("Ana", "Silva", "TAX-1", "ana@example.com", "910000000");
        Document document = new Document("DOC-1", "Passport", LocalDate.of(2030, 1, 1));
        client.addDocument(document);
        ReflectionTestUtils.setField(client, "id", 10L);
        ReflectionTestUtils.setField(document, "id", 20L);

        ClientResponse response = mapper.toResponse(client);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.firstName()).isEqualTo("Ana");
        assertThat(response.lastName()).isEqualTo("Silva");
        assertThat(response.taxIdentifier()).isEqualTo("TAX-1");
        assertThat(response.email()).isEqualTo("ana@example.com");
        assertThat(response.phoneNumber()).isEqualTo("910000000");
        assertThat(response.documents()).hasSize(1);
        assertThat(response.documents().getFirst().id()).isEqualTo(20L);
        assertThat(response.documents().getFirst().number()).isEqualTo("DOC-1");
        assertThat(response.documents().getFirst().description()).isEqualTo("Passport");
        assertThat(response.documents().getFirst().expirationDate()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(response.documents().getFirst().clientId()).isEqualTo(10L);
    }

    @Test
    void updateEntity_mapsUpdateClientRequestToClient() {
        Client client = new Client("Ana", "Silva", "TAX-1", "ana@example.com", "910000000");
        var request = new UpdateClientRequest(
                "Bea", "Costa", "TAX-2", "bea@example.com", "920000000");

        mapper.updateEntity(client, request);

        assertThat(client.getFirstName()).isEqualTo("Bea");
        assertThat(client.getLastName()).isEqualTo("Costa");
        assertThat(client.getTaxIdentifier()).isEqualTo("TAX-2");
        assertThat(client.getEmail()).isEqualTo("bea@example.com");
        assertThat(client.getPhoneNumber()).isEqualTo("920000000");
    }
}
