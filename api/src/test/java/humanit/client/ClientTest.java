package humanit.client;

import humanit.document.Document;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientTest {
    @Test
    void addDocument_setsBothSidesOfRelationship() {
        Client client = new Client("Ana", "Silva", "TAX-1", "ana@example.com", "910000000");

        Document document = new Document("DOC-1", "Passport", LocalDate.of(2030, 1, 1));

        client.addDocument(document);

        assertThat(client.getDocuments()).containsExactly(document);
        assertThat(document.getClient()).isSameAs(client);
    }

    @Test
    void removeDocument_clearsBothSidesOfRelationship() {
        Client client = new Client("Ana", "Silva", "TAX-1", "ana@example.com", "910000000");
        Document document = new Document("DOC-1", "Passport", LocalDate.of(2030, 1, 1));

        client.addDocument(document);
        client.removeDocument(document);

        assertThat(client.getDocuments()).doesNotContain(document);
        assertThat(document.getClient()).isNull();
    }
}
