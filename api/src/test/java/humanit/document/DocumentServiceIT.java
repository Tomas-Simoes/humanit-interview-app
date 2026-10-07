package humanit.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;

import humanit.client.ClientService;
import humanit.client.dto.CreateClientRequest;
import humanit.document.dto.CreateDocumentRequest;
import humanit.document.dto.UpdateDocumentRequest;
import humanit.error.ApplicationException;
import humanit.error.ErrorCode;

@SpringBootTest
@Sql(statements = {
        "DELETE FROM documents",
        "DELETE FROM clients"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DocumentServiceIT {
    @Autowired
    ClientService clientService;

    @Autowired
    DocumentService documentService;

    @Autowired
    DocumentRepository documentRepository;

    @Test
    void createDocumentPersistsDocumentForClient() {
        Long clientId = createClient("TAX-1", "ana@example.com");

        var response = documentService.createDocument(clientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));

        assertThat(response.id()).isNotNull();
        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(documentRepository.existsByClientIdAndNumber(clientId, "DOC-1")).isTrue();
    }

    @Test
    void createDocumentRejectsMissingClient() {
        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.createDocument(999L, documentRequest(
                        "DOC-1", "Passport", LocalDate.of(2030, 1, 1))))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));

        assertThat(documentRepository.findAll()).isEmpty();
    }

    @Test
    void createDocumentRejectsDuplicateNumberForSameClient() {
        Long clientId = createClient("TAX-1", "ana@example.com");
        documentService.createDocument(clientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.createDocument(clientId, documentRequest(
                        "DOC-1", "Identity card", LocalDate.of(2031, 1, 1))))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.DOCUMENT_NUMBER_EXISTS));

        assertThat(documentRepository.findByClientId(clientId)).hasSize(1);
    }

    @Test
    void createDocumentAllowsSameNumberForDifferentClients() {
        Long firstClientId = createClient("TAX-1", "ana@example.com");
        Long secondClientId = createClient("TAX-2", "bob@example.com");

        documentService.createDocument(firstClientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));
        documentService.createDocument(secondClientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2031, 1, 1)));

        assertThat(documentRepository.findByClientId(firstClientId)).hasSize(1);
        assertThat(documentRepository.findByClientId(secondClientId)).hasSize(1);
    }

    @Test
    void getDocumentReturnsDocumentForOwningClient() {
        Long clientId = createClient("TAX-1", "ana@example.com");
        var created = documentService.createDocument(clientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));

        var found = documentService.getDocument(clientId, created.id());

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.clientId()).isEqualTo(clientId);
        assertThat(found.number()).isEqualTo("DOC-1");
    }

    @Test
    void getDocumentRejectsMissingClient() {
        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.getDocument(999L, 1L))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }

    @Test
    void getDocumentRejectsMissingDocument() {
        Long clientId = createClient("TAX-1", "ana@example.com");

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.getDocument(clientId, 999L))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    @Test
    void getDocumentRejectsDocumentOwnedByAnotherClient() {
        Long firstClientId = createClient("TAX-1", "ana@example.com");
        Long secondClientId = createClient("TAX-2", "bob@example.com");
        var document = documentService.createDocument(firstClientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.getDocument(secondClientId, document.id()))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    @Test
    void updateDocumentPersistsChanges() {
        Long clientId = createClient("TAX-1", "ana@example.com");
        var created = documentService.createDocument(clientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));

        var response = documentService.updateDocument(clientId, created.id(), documentUpdateRequest(
                "DOC-2", "Identity card", LocalDate.of(2031, 2, 2)));

        assertThat(response.id()).isEqualTo(created.id());

        Document saved = documentRepository.findById(created.id()).orElseThrow();
        assertThat(saved.getNumber()).isEqualTo("DOC-2");
    }

    @Test
    void updateDocumentAllowsKeepingCurrentNumber() {
        Long clientId = createClient("TAX-1", "ana@example.com");
        var created = documentService.createDocument(clientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));

        var response = documentService.updateDocument(clientId, created.id(), documentUpdateRequest(
                "DOC-1", "Updated passport", LocalDate.of(2031, 1, 1)));

        assertThat(response.id()).isEqualTo(created.id());
        assertThat(documentRepository.existsByClientIdAndNumber(clientId, "DOC-1")).isTrue();
    }

    @Test
    void updateDocumentRejectsDuplicateNumberForSameClient() {
        Long clientId = createClient("TAX-1", "ana@example.com");
        documentService.createDocument(clientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));
        var second = documentService.createDocument(clientId, documentRequest(
                "DOC-2", "Identity card", LocalDate.of(2031, 2, 2)));

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.updateDocument(clientId, second.id(), documentUpdateRequest(
                        "DOC-1", "Identity card", LocalDate.of(2031, 2, 2))))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.DOCUMENT_NUMBER_EXISTS));
    }

    @Test
    void updateDocumentRejectsMissingClient() {
        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.updateDocument(999L, 1L, documentUpdateRequest(
                        "DOC-1", "Passport", LocalDate.of(2030, 1, 1))))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }

    @Test
    void updateDocumentRejectsMissingDocument() {
        Long clientId = createClient("TAX-1", "ana@example.com");

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.updateDocument(clientId, 999L, documentUpdateRequest(
                        "DOC-1", "Passport", LocalDate.of(2030, 1, 1))))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    @Test
    void updateDocumentRejectsDocumentOwnedByAnotherClient() {
        Long firstClientId = createClient("TAX-1", "ana@example.com");
        Long secondClientId = createClient("TAX-2", "bob@example.com");
        var document = documentService.createDocument(firstClientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.updateDocument(secondClientId, document.id(), documentUpdateRequest(
                        "DOC-2", "Identity card", LocalDate.of(2031, 2, 2))))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    @Test
    void deleteDocumentRemovesDocument() {
        Long clientId = createClient("TAX-1", "ana@example.com");
        var document = documentService.createDocument(clientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));

        documentService.deleteDocument(clientId, document.id());

        assertThat(documentRepository.findById(document.id())).isEmpty();
    }

    @Test
    void deleteDocumentRejectsMissingClient() {
        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.deleteDocument(999L, 1L))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }

    @Test
    void deleteDocumentRejectsMissingDocument() {
        Long clientId = createClient("TAX-1", "ana@example.com");

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.deleteDocument(clientId, 999L))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    @Test
    void deleteDocumentDoesNotDeleteDocumentOwnedByDifferentClient() {
        Long firstClientId = createClient("TAX-1", "ana@example.com");
        Long secondClientId = createClient("TAX-2", "bob@example.com");
        var document = documentService.createDocument(firstClientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));

        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.deleteDocument(secondClientId, document.id()))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND));

        assertThat(documentRepository.findById(document.id())).isPresent();
    }

    @Test
    void getDocumentsReturnsOnlyDocumentsForClient() {
        Long firstClientId = createClient("TAX-1", "ana@example.com");
        Long secondClientId = createClient("TAX-2", "bob@example.com");
        documentService.createDocument(firstClientId, documentRequest(
                "DOC-1", "Passport", LocalDate.of(2030, 1, 1)));
        documentService.createDocument(firstClientId, documentRequest(
                "DOC-2", "Identity card", LocalDate.of(2031, 1, 1)));
        documentService.createDocument(secondClientId, documentRequest(
                "DOC-3", "Passport", LocalDate.of(2032, 1, 1)));

        var page = documentService.getDocuments(
                firstClientId,
                PageRequest.of(0, 20, Sort.by("number").ascending()));

        assertThat(page.getContent())
                .extracting(summary -> summary.number())
                .containsExactly("DOC-1", "DOC-2");
        assertThat(page.getContent())
                .extracting(summary -> summary.clientId())
                .containsOnly(firstClientId);
    }

    @Test
    void getDocumentsRejectsMissingClient() {
        assertThatExceptionOfType(ApplicationException.class)
                .isThrownBy(() -> documentService.getDocuments(999L, PageRequest.of(0, 20)))
                .satisfies(e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }

    private Long createClient(String taxIdentifier, String email) {
        return clientService.createClient(new CreateClientRequest(
                "Ana", "Silva", taxIdentifier, email, "910000000")).id();
    }

    private CreateDocumentRequest documentRequest(String number, String description, LocalDate expirationDate) {
        return new CreateDocumentRequest(number, description, expirationDate);
    }

    private UpdateDocumentRequest documentUpdateRequest(String number, String description, LocalDate expirationDate) {
        return new UpdateDocumentRequest(number, description, expirationDate);
    }
}
