package humanit.document;

import static org.assertj.core.api.Assertions.assertThat;

import humanit.client.Client;
import humanit.document.dto.CreateDocumentRequest;
import humanit.document.dto.DocumentResponse;
import humanit.document.dto.UpdateDocumentRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DocumentMapperTest {
  private final DocumentMapper mapper = new DocumentMapper();

  @Test
  void toEntity_mapsCreateDocumentRequestToDocument() {
    var request = new CreateDocumentRequest("DOC-1", "Passport", LocalDate.of(2030, 1, 1));

    Document document = mapper.toEntity(request);

    assertThat(document.getNumber()).isEqualTo("DOC-1");
    assertThat(document.getDescription()).isEqualTo("Passport");
    assertThat(document.getExpirationDate()).isEqualTo(LocalDate.of(2030, 1, 1));
  }

  @Test
  void toEntity_usesNormalizedDocumentInput() {
    var request = new CreateDocumentRequest(" doc-1 ", " Passport ", LocalDate.of(2030, 1, 1));

    Document document = mapper.toEntity(request);

    assertThat(document.getNumber()).isEqualTo("DOC-1");
    assertThat(document.getDescription()).isEqualTo("Passport");
  }

  @Test
  void toResponse_mapsDocumentToDocumentResponse() {
    Client client = new Client("Ana", "Silva", "123456789", "ana@example.com", "+351910000000");
    Document document = new Document("DOC-1", "Passport", LocalDate.of(2030, 1, 1));
    client.addDocument(document);
    ReflectionTestUtils.setField(client, "id", 10L);
    ReflectionTestUtils.setField(document, "id", 20L);

    DocumentResponse response = mapper.toResponse(document);

    assertThat(response.id()).isEqualTo(20L);
    assertThat(response.number()).isEqualTo("DOC-1");
    assertThat(response.description()).isEqualTo("Passport");
    assertThat(response.expirationDate()).isEqualTo(LocalDate.of(2030, 1, 1));
    assertThat(response.clientId()).isEqualTo(10L);
  }

  @Test
  void updateEntity_mapsUpdateDocumentRequestToDocument() {
    Document document = new Document("DOC-1", "Passport", LocalDate.of(2030, 1, 1));
    var request = new UpdateDocumentRequest("DOC-2", "Identity card", LocalDate.of(2031, 2, 2));

    mapper.updateEntity(document, request);

    assertThat(document.getNumber()).isEqualTo("DOC-2");
    assertThat(document.getDescription()).isEqualTo("Identity card");
    assertThat(document.getExpirationDate()).isEqualTo(LocalDate.of(2031, 2, 2));
  }
}
