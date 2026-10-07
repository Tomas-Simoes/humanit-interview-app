package humanit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import humanit.client.dto.CreateClientRequest;
import humanit.client.dto.UpdateClientRequest;
import humanit.error.ApplicationException;
import humanit.error.ErrorCode;

@SpringBootTest
@Sql(statements = {
        "DELETE FROM documents",
        "DELETE FROM clients"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ClientServiceIT {
    @Autowired
    ClientService clientService;

    @Autowired
    ClientRepository clientRepository;

    @Test
    void createClientPersistsClient() {
        var response = clientService.createClient(clientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        assertThat(response.id()).isNotNull();

        Client saved = clientRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getTaxIdentifier()).isEqualTo("TAX-1");
    }

    @Test
    void createClientRejectsDuplicateEmail() {
        clientService.createClient(clientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> clientService.createClient(clientRequest(
                        "Bob", "Costa", "TAX-2", "ana@example.com", "920000000")))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_EMAIL_EXISTS));

        assertThat(clientRepository.findAll()).hasSize(1);
    }

    @Test
    void createClientRejectsDuplicateTaxIdentifier() {
        clientService.createClient(clientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> clientService.createClient(clientRequest(
                        "Bob", "Costa", "TAX-1", "bob@example.com", "920000000")))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_TAX_IDENTIFIER_EXISTS));

        assertThat(clientRepository.findAll()).hasSize(1);
    }

    @Test
    void getClientReturnsPersistedClient() {
        var created = clientService.createClient(clientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        var found = clientService.getClient(created.id());

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.email()).isEqualTo("ana@example.com");
    }

    @Test
    void getClientRejectsMissingClient() {
        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> clientService.getClient(999L))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }

    @Test
    void updateClientPersistsChanges() {
        var created = clientService.createClient(clientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        var response = clientService.updateClient(created.id(), clientUpdateRequest(
                "Bea", "Costa", "TAX-2", "bea@example.com", "920000000"));

        assertThat(response.id()).isEqualTo(created.id());

        Client saved = clientRepository.findById(created.id()).orElseThrow();
        assertThat(saved.getTaxIdentifier()).isEqualTo("TAX-2");
        assertThat(saved.getEmail()).isEqualTo("bea@example.com");
    }

    @Test
    void updateClientAllowsKeepingCurrentEmailAndTaxIdentifier() {
        var created = clientService.createClient(clientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        var response = clientService.updateClient(created.id(), clientUpdateRequest(
                "Ana", "Costa", "TAX-1", "ana@example.com", "920000000"));

        assertThat(response.id()).isEqualTo(created.id());

        Client saved = clientRepository.findById(created.id()).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getTaxIdentifier()).isEqualTo("TAX-1");
    }

    @Test
    void updateClientRejectsMissingClient() {
        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> clientService.updateClient(999L, clientUpdateRequest(
                        "Ana", "Silva", "TAX-1", "ana@example.com", "910000000")))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }

    @Test
    void updateClientRejectsDuplicateEmail() {
        clientService.createClient(clientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));
        var second = clientService.createClient(clientRequest(
                "Bob", "Costa", "TAX-2", "bob@example.com", "920000000"));

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> clientService.updateClient(second.id(), clientUpdateRequest(
                        "Bob", "Costa", "TAX-2", "ana@example.com", "920000000")))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_EMAIL_EXISTS));
    }

    @Test
    void updateClientRejectsDuplicateTaxIdentifier() {
        clientService.createClient(clientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));
        var second = clientService.createClient(clientRequest(
                "Bob", "Costa", "TAX-2", "bob@example.com", "920000000"));

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> clientService.updateClient(second.id(), clientUpdateRequest(
                        "Bob", "Costa", "TAX-1", "bob@example.com", "920000000")))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_TAX_IDENTIFIER_EXISTS));
    }

    @Test
    void deleteClientRemovesClient() {
        var created = clientService.createClient(clientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        clientService.deleteClient(created.id());

        assertThat(clientRepository.findById(created.id())).isEmpty();
    }

    @Test
    void deleteClientRejectsMissingClient() {
        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> clientService.deleteClient(999L))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }

    @Test
    void getClientsReturnsClientSummaries() {
        clientService.createClient(clientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        var page = clientService.getClients(PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).email()).isEqualTo("ana@example.com");
        assertThat(page.getContent().get(0).documentCount()).isZero();
    }

    private CreateClientRequest clientRequest(
            String firstName,
            String lastName,
            String taxIdentifier,
            String email,
            String phoneNumber) {
        return new CreateClientRequest(firstName, lastName, taxIdentifier, email, phoneNumber);
    }

    private UpdateClientRequest clientUpdateRequest(
            String firstName,
            String lastName,
            String taxIdentifier,
            String email,
            String phoneNumber) {
        return new UpdateClientRequest(firstName, lastName, taxIdentifier, email, phoneNumber);
    }
}
