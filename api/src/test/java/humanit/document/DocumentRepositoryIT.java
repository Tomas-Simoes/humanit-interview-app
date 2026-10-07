package humanit.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import humanit.client.Client;
import humanit.client.ClientRepository;

@DataJpaTest
class DocumentRepositoryIT {
    @Autowired
    ClientRepository clientRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Test
    void duplicateDocumentNumberForSameClientViolatesDatabaseConstraint() {
        Client client = saveClient("TAX-1", "ana@example.com");
        saveDocument(client, "DOC-1", LocalDate.of(2030, 1, 1));

        assertThatThrownBy(() -> saveDocument(client, "DOC-1", LocalDate.of(2031, 1, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameDocumentNumberForDifferentClientsIsAllowed() {
        Client firstClient = saveClient("TAX-1", "ana@example.com");
        Client secondClient = saveClient("TAX-2", "bob@example.com");

        saveDocument(firstClient, "DOC-1", LocalDate.of(2030, 1, 1));
        saveDocument(secondClient, "DOC-1", LocalDate.of(2031, 1, 1));

        assertThat(documentRepository.findByClientId(firstClient.getId())).hasSize(1);
        assertThat(documentRepository.findByClientId(secondClient.getId())).hasSize(1);
    }

    @Test
    void findByIdAndClientIdReturnsDocumentForOwningClient() {
        Client client = saveClient("TAX-1", "ana@example.com");
        Document document = saveDocument(client, "DOC-1", LocalDate.of(2030, 1, 1));

        var found = documentRepository.findByIdAndClientId(document.getId(), client.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getNumber()).isEqualTo("DOC-1");
    }

    @Test
    void findByIdAndClientIdReturnsEmptyForDifferentClient() {
        Client firstClient = saveClient("TAX-1", "ana@example.com");
        Client secondClient = saveClient("TAX-2", "bob@example.com");
        Document document = saveDocument(firstClient, "DOC-1", LocalDate.of(2030, 1, 1));

        var found = documentRepository.findByIdAndClientId(document.getId(), secondClient.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void existsByClientIdAndNumberReturnsTrueOnlyForOwningClient() {
        Client firstClient = saveClient("TAX-1", "ana@example.com");
        Client secondClient = saveClient("TAX-2", "bob@example.com");
        saveDocument(firstClient, "DOC-1", LocalDate.of(2030, 1, 1));

        assertThat(documentRepository.existsByClientIdAndNumber(firstClient.getId(), "DOC-1")).isTrue();
        assertThat(documentRepository.existsByClientIdAndNumber(secondClient.getId(), "DOC-1")).isFalse();
    }

    @Test
    void existsByClientIdAndNumberAndIdNotIgnoresCurrentDocument() {
        Client client = saveClient("TAX-1", "ana@example.com");
        Client otherClient = saveClient("TAX-2", "bob@example.com");
        Document first = saveDocument(client, "DOC-1", LocalDate.of(2030, 1, 1));
        Document second = saveDocument(client, "DOC-2", LocalDate.of(2031, 1, 1));
        saveDocument(otherClient, "DOC-2", LocalDate.of(2032, 1, 1));

        assertThat(documentRepository.existsByClientIdAndNumberAndIdNot(
                client.getId(), "DOC-1", first.getId())).isFalse();
        assertThat(documentRepository.existsByClientIdAndNumberAndIdNot(
                client.getId(), "DOC-2", first.getId())).isTrue();
        assertThat(documentRepository.existsByClientIdAndNumberAndIdNot(
                otherClient.getId(), "DOC-2", second.getId())).isTrue();
    }

    @Test
    void findDocumentSummariesByClientIdReturnsOnlyClientDocuments() {
        Client firstClient = saveClient("TAX-1", "ana@example.com");
        Client secondClient = saveClient("TAX-2", "bob@example.com");
        saveDocument(firstClient, "DOC-1", LocalDate.of(2030, 1, 1));
        saveDocument(firstClient, "DOC-2", LocalDate.of(2031, 1, 1));
        saveDocument(secondClient, "DOC-3", LocalDate.of(2032, 1, 1));

        var page = documentRepository.findDocumentSummariesByClientId(
                firstClient.getId(),
                PageRequest.of(0, 20, Sort.by("number").ascending()));

        assertThat(page.getContent())
                .extracting(summary -> summary.number())
                .containsExactly("DOC-1", "DOC-2");
        assertThat(page.getContent())
                .extracting(summary -> summary.clientId())
                .containsOnly(firstClient.getId());
    }

    @Test
    void findDocumentSummariesByClientIdSortsByExpirationDate() {
        Client client = saveClient("TAX-1", "ana@example.com");
        saveDocument(client, "DOC-3", LocalDate.of(2032, 1, 1));
        saveDocument(client, "DOC-1", LocalDate.of(2030, 1, 1));
        saveDocument(client, "DOC-2", LocalDate.of(2031, 1, 1));

        var page = documentRepository.findDocumentSummariesByClientId(
                client.getId(),
                PageRequest.of(0, 20, Sort.by("expirationDate").ascending()));

        assertThat(page.getContent())
                .extracting(summary -> summary.number())
                .containsExactly("DOC-1", "DOC-2", "DOC-3");
    }

    @Test
    void findDocumentSummariesByClientIdPaginates() {
        Client client = saveClient("TAX-1", "ana@example.com");
        saveDocument(client, "DOC-1", LocalDate.of(2030, 1, 1));
        saveDocument(client, "DOC-2", LocalDate.of(2031, 1, 1));
        saveDocument(client, "DOC-3", LocalDate.of(2032, 1, 1));

        var page = documentRepository.findDocumentSummariesByClientId(
                client.getId(),
                PageRequest.of(1, 1, Sort.by("expirationDate").ascending()));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent())
                .extracting(summary -> summary.number())
                .containsExactly("DOC-2");
    }

    private Client saveClient(String taxIdentifier, String email) {
        return clientRepository.saveAndFlush(
                new Client("Ana", "Silva", taxIdentifier, email, "910000000"));
    }

    private Document saveDocument(Client client, String number, LocalDate expirationDate) {
        Document document = new Document(number, "Passport", expirationDate);
        client.addDocument(document);
        return documentRepository.saveAndFlush(document);
    }
}
