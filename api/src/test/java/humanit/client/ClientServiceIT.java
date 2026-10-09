package humanit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import humanit.client.dto.CreateClientRequest;
import humanit.client.dto.UpdateClientRequest;
import humanit.document.DocumentRepository;
import humanit.document.dto.CreateDocumentRequest;
import humanit.error.ApplicationException;
import humanit.error.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Sql(
    statements = {"DELETE FROM documents", "DELETE FROM clients"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ClientServiceIT {
  @Autowired ClientService clientService;

  @Autowired ClientRepository clientRepository;

  @Autowired DocumentRepository documentRepository;

  @Test
  void createClientPersistsClient() {
    var response =
        clientService.createClient(
            clientRequest("Ana", "Silva", "123456789", "ana@example.com", "+351910000000"));

    assertThat(response.id()).isNotNull();

    Client saved = clientRepository.findById(response.id()).orElseThrow();
    assertThat(saved.getEmail()).isEqualTo("ana@example.com");
    assertThat(saved.getTaxIdentifier()).isEqualTo("123456789");
  }

  @Test
  void createClientPersistsDocumentsWithClient() {
    var response =
        clientService.createClient(
            new CreateClientRequest(
                "Ana",
                "Silva",
                "123456789",
                "ana@example.com",
                "+351910000000",
                List.of(
                    documentRequest("DOC-1", "Passport", LocalDate.of(2030, 1, 1)),
                    documentRequest("DOC-2", "Identity card", LocalDate.of(2031, 2, 2)))));

    assertThat(response.id()).isNotNull();
    assertThat(response.documents())
        .extracting(document -> document.number())
        .containsExactly("DOC-1", "DOC-2");
    assertThat(response.documents())
        .extracting(document -> document.clientId())
        .containsOnly(response.id());
    assertThat(documentRepository.findByClientId(response.id())).hasSize(2);
  }

  @Test
  void createClientRejectsDuplicateDocumentNumbersAndRollsBackClient() {
    var request =
        new CreateClientRequest(
            "Ana",
            "Silva",
            "123456789",
            "ana@example.com",
            "+351910000000",
            List.of(
                documentRequest("DOC-1", "Passport", LocalDate.of(2030, 1, 1)),
                documentRequest("DOC-1", "Identity card", LocalDate.of(2031, 2, 2))));

    assertThatExceptionOfType(ApplicationException.class)
        .isThrownBy(() -> clientService.createClient(request))
        .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.DOCUMENT_NUMBER_EXISTS));

    assertThat(clientRepository.findAll()).isEmpty();
    assertThat(documentRepository.findAll()).isEmpty();
  }

  @Test
  void createClientRejectsDuplicateEmail() {
    clientService.createClient(
        clientRequest("Ana", "Silva", "123456789", "ana@example.com", "+351910000000"));

    assertThatExceptionOfType(ApplicationException.class)
        .isThrownBy(
            () ->
                clientService.createClient(
                    clientRequest("Bob", "Costa", "987654321", "ana@example.com", "+351920000000")))
        .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_EMAIL_EXISTS));

    assertThat(clientRepository.findAll()).hasSize(1);
  }

  @Test
  void createClientRejectsDuplicateTaxIdentifier() {
    clientService.createClient(
        clientRequest("Ana", "Silva", "123456789", "ana@example.com", "+351910000000"));

    assertThatExceptionOfType(ApplicationException.class)
        .isThrownBy(
            () ->
                clientService.createClient(
                    clientRequest("Bob", "Costa", "123456789", "bob@example.com", "+351920000000")))
        .satisfies(
            e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_TAX_IDENTIFIER_EXISTS));

    assertThat(clientRepository.findAll()).hasSize(1);
  }

  @Test
  void getClientReturnsPersistedClient() {
    var created =
        clientService.createClient(
            clientRequest("Ana", "Silva", "123456789", "ana@example.com", "+351910000000"));

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
    var created =
        clientService.createClient(
            clientRequest("Ana", "Silva", "123456789", "ana@example.com", "+351910000000"));

    var response =
        clientService.updateClient(
            created.id(),
            clientUpdateRequest("Bea", "Costa", "987654321", "bea@example.com", "+351920000000"));

    assertThat(response.id()).isEqualTo(created.id());

    Client saved = clientRepository.findById(created.id()).orElseThrow();
    assertThat(saved.getTaxIdentifier()).isEqualTo("987654321");
    assertThat(saved.getEmail()).isEqualTo("bea@example.com");
  }

  @Test
  void updateClientAllowsKeepingCurrentEmailAndTaxIdentifier() {
    var created =
        clientService.createClient(
            clientRequest("Ana", "Silva", "123456789", "ana@example.com", "+351910000000"));

    var response =
        clientService.updateClient(
            created.id(),
            clientUpdateRequest("Ana", "Costa", "123456789", "ana@example.com", "+351920000000"));

    assertThat(response.id()).isEqualTo(created.id());

    Client saved = clientRepository.findById(created.id()).orElseThrow();
    assertThat(saved.getEmail()).isEqualTo("ana@example.com");
    assertThat(saved.getTaxIdentifier()).isEqualTo("123456789");
  }

  @Test
  void updateClientRejectsMissingClient() {
    assertThatExceptionOfType(ApplicationException.class)
        .isThrownBy(
            () ->
                clientService.updateClient(
                    999L,
                    clientUpdateRequest(
                        "Ana", "Silva", "123456789", "ana@example.com", "+351910000000")))
        .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
  }

  @Test
  void updateClientRejectsDuplicateEmail() {
    clientService.createClient(
        clientRequest("Ana", "Silva", "123456789", "ana@example.com", "+351910000000"));
    var second =
        clientService.createClient(
            clientRequest("Bob", "Costa", "987654321", "bob@example.com", "+351920000000"));

    assertThatExceptionOfType(ApplicationException.class)
        .isThrownBy(
            () ->
                clientService.updateClient(
                    second.id(),
                    clientUpdateRequest(
                        "Bob", "Costa", "987654321", "ana@example.com", "+351920000000")))
        .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_EMAIL_EXISTS));
  }

  @Test
  void updateClientRejectsDuplicateTaxIdentifier() {
    clientService.createClient(
        clientRequest("Ana", "Silva", "123456789", "ana@example.com", "+351910000000"));
    var second =
        clientService.createClient(
            clientRequest("Bob", "Costa", "987654321", "bob@example.com", "+351920000000"));

    assertThatExceptionOfType(ApplicationException.class)
        .isThrownBy(
            () ->
                clientService.updateClient(
                    second.id(),
                    clientUpdateRequest(
                        "Bob", "Costa", "123456789", "bob@example.com", "+351920000000")))
        .satisfies(
            e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_TAX_IDENTIFIER_EXISTS));
  }

  @Test
  void deleteClientRemovesClient() {
    var created =
        clientService.createClient(
            clientRequest("Ana", "Silva", "123456789", "ana@example.com", "+351910000000"));

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
    clientService.createClient(
        clientRequest("Ana", "Silva", "123456789", "ana@example.com", "+351910000000"));

    var page = clientService.getClients(PageRequest.of(0, 20));

    assertThat(page.getContent()).hasSize(1);
    assertThat(page.getContent().get(0).email()).isEqualTo("ana@example.com");
    assertThat(page.getContent().get(0).documentCount()).isZero();
  }

  private CreateClientRequest clientRequest(
      String firstName, String lastName, String taxIdentifier, String email, String phoneNumber) {
    return new CreateClientRequest(firstName, lastName, taxIdentifier, email, phoneNumber);
  }

  private UpdateClientRequest clientUpdateRequest(
      String firstName, String lastName, String taxIdentifier, String email, String phoneNumber) {
    return new UpdateClientRequest(firstName, lastName, taxIdentifier, email, phoneNumber);
  }

  private CreateDocumentRequest documentRequest(
      String number, String description, LocalDate expirationDate) {
    return new CreateDocumentRequest(number, description, expirationDate);
  }
}
