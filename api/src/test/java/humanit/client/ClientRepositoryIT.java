package humanit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import humanit.document.Document;
import humanit.document.DocumentRepository;

@DataJpaTest
class ClientRepositoryIT {
    @Autowired
    ClientRepository clientRepository;
    @Autowired
    DocumentRepository documentRepository;

    @Test
    void duplicateEmailViolatesDatabaseConstraint() {
        clientRepository.saveAndFlush(
                new Client("Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        assertThatThrownBy(() -> clientRepository.saveAndFlush(
                new Client("Bob", "Costa", "TAX-2", "ana@example.com", "920000000")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateTaxIdentifierViolatesDatabaseConstraint() {
        clientRepository.saveAndFlush(
                new Client("Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        assertThatThrownBy(() -> clientRepository.saveAndFlush(
                new Client("Bob", "Costa", "TAX-1", "bob@example.com", "920000000")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findClientSummariesIncludesDocumentCount() {
        Client client = clientRepository.save(
                new Client("Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        Document document = new Document("DOC-1", "Passport", LocalDate.of(2030, 1, 1));
        client.addDocument(document);
        documentRepository.saveAndFlush(document);

        var page = clientRepository.findClientSummaries(PageRequest.of(0, 20));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).documentCount()).isEqualTo(1);
    }

    @Test
    void findClientSummariesIncludesClientsWithZeroDocuments() {
        clientRepository.saveAndFlush(
                new Client("Ana", "Silva", "TAX-1", "ana@example.com", "910000000"));

        var page = clientRepository.findClientSummaries(PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).documentCount()).isZero();
    }

    @Test
    void findClientSummariesSortsAndPaginates() {
        clientRepository.save(new Client("Zoe", "Zulu", "TAX-1", "zoe@example.com", "910000000"));
        clientRepository.save(new Client("Ana", "Alpha", "TAX-2", "ana@example.com", "920000000"));
        clientRepository.saveAndFlush(new Client("Mia", "Middle", "TAX-3", "mia@example.com", "930000000"));

        var page = clientRepository.findClientSummaries(
                PageRequest.of(0, 2, Sort.by("lastName").ascending()));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(summary -> summary.lastName())
                .containsExactly("Alpha", "Middle");
    }

    @Test
    void deleteClientDeletesDocuments() {
        Client client = new Client("Ana", "Silva", "TAX-1", "ana@example.com", "910000000");
        Document document = new Document("DOC-1", "Passport", LocalDate.of(2030, 1, 1));
        client.addDocument(document);

        Client savedClient = clientRepository.saveAndFlush(client);
        Long documentId = savedClient.getDocuments().getFirst().getId();

        assertThat(documentRepository.existsById(documentId)).isTrue();

        clientRepository.delete(savedClient);
        clientRepository.flush();

        assertThat(documentRepository.existsById(documentId)).isFalse();
    }
}
