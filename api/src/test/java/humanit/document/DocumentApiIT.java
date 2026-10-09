package humanit.document;

import static humanit.helper.ApiAssertions.assertProblem;
import static humanit.helper.ApiAssertions.assertValidationError;
import static humanit.helper.ApiAssertions.parseJson;
import static humanit.helper.AuthTestSupport.authenticate;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import humanit.client.dto.ClientResponse;
import humanit.client.dto.CreateClientRequest;
import humanit.document.dto.CreateDocumentRequest;
import humanit.document.dto.DocumentResponse;
import humanit.document.dto.UpdateDocumentRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(
    statements = {"DELETE FROM app_users", "DELETE FROM documents", "DELETE FROM clients"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DocumentApiIT {
  @Autowired TestRestTemplate rest;

  @BeforeEach
  void authenticateRequests() {
    authenticate(rest);
  }

  @Test
  void createDocumentReturns201AndLocationHeader() {
    Long clientId = createClient("123456789", "ana@example.com");

    var response =
        rest.postForEntity(
            "/api/v1/clients/" + clientId + "/documents",
            documentRequest("DOC-1", "Passport", LocalDate.of(2030, 1, 1)),
            DocumentResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getHeaders().getLocation()).isNotNull();
    assertThat(response.getHeaders().getLocation().toString())
        .endsWith("/api/v1/clients/" + clientId + "/documents/" + response.getBody().id());
    assertThat(response.getBody().number()).isEqualTo("DOC-1");
    assertThat(response.getBody().clientId()).isEqualTo(clientId);
  }

  @Test
  void createDocumentThenFetchDocumentById() {
    Long clientId = createClient("123456789", "ana@example.com");
    DocumentResponse created = createDocument(clientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var response =
        rest.getForEntity(
            "/api/v1/clients/" + clientId + "/documents/" + created.id(), DocumentResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(created.id());
    assertThat(response.getBody().number()).isEqualTo("DOC-1");
    assertThat(response.getBody().clientId()).isEqualTo(clientId);
  }

  @Test
  void createDocumentForMissingClientReturns404() {
    var response =
        rest.postForEntity(
            "/api/v1/clients/999/documents",
            documentRequest("DOC-1", "Passport", LocalDate.of(2030, 1, 1)),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertProblem(
        response.getBody(),
        HttpStatus.NOT_FOUND,
        "/problems/client-not-found",
        "Client not found",
        "CLIENT_NOT_FOUND");
  }

  @Test
  void createDocumentWithInvalidBodyReturns400() {
    Long clientId = createClient("123456789", "ana@example.com");

    var response =
        rest.postForEntity(
            "/api/v1/clients/" + clientId + "/documents",
            documentRequest("", "Passport", LocalDate.of(2030, 1, 1)),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode problem =
        assertProblem(
            response.getBody(),
            HttpStatus.BAD_REQUEST,
            "/problems/validation-failed",
            "Validation failed",
            "VALIDATION_FAILED");
    assertValidationError(problem, "number");
  }

  @Test
  void createDocumentWithDuplicateNumberForSameClientReturns409() {
    Long clientId = createClient("123456789", "ana@example.com");
    createDocument(clientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var response =
        rest.postForEntity(
            "/api/v1/clients/" + clientId + "/documents",
            documentRequest("DOC-1", "Identity card", LocalDate.of(2031, 1, 1)),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertProblem(
        response.getBody(),
        HttpStatus.CONFLICT,
        "/problems/document-number-already-exists",
        "Document number already exists",
        "DOCUMENT_NUMBER_EXISTS");
  }

  @Test
  void createDocumentNormalizesNumberForDuplicateChecks() {
    Long clientId = createClient("123456789", "ana@example.com");
    createDocument(clientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var response =
        rest.postForEntity(
            "/api/v1/clients/" + clientId + "/documents",
            documentRequest(" doc-1 ", "Identity card", LocalDate.of(2031, 1, 1)),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertProblem(
        response.getBody(),
        HttpStatus.CONFLICT,
        "/problems/document-number-already-exists",
        "Document number already exists",
        "DOCUMENT_NUMBER_EXISTS");
  }

  @Test
  void getMissingDocumentReturns404() {
    Long clientId = createClient("123456789", "ana@example.com");

    var response =
        rest.getForEntity("/api/v1/clients/" + clientId + "/documents/999", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertProblem(
        response.getBody(),
        HttpStatus.NOT_FOUND,
        "/problems/document-not-found",
        "Document not found",
        "DOCUMENT_NOT_FOUND");
  }

  @Test
  void getDocumentOwnedByDifferentClientReturns404() {
    Long firstClientId = createClient("123456789", "ana@example.com");
    Long secondClientId = createClient("987654321", "bob@example.com");
    DocumentResponse document = createDocument(firstClientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var response =
        rest.getForEntity(
            "/api/v1/clients/" + secondClientId + "/documents/" + document.id(), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertProblem(
        response.getBody(),
        HttpStatus.NOT_FOUND,
        "/problems/document-not-found",
        "Document not found",
        "DOCUMENT_NOT_FOUND");
  }

  @Test
  void updateDocumentReturns200AndUpdatedBody() {
    Long clientId = createClient("123456789", "ana@example.com");
    DocumentResponse document = createDocument(clientId, "DOC-1", LocalDate.of(2030, 1, 1));
    var update = new UpdateDocumentRequest("DOC-2", "Identity card", LocalDate.of(2031, 2, 2));

    var response =
        rest.exchange(
            "/api/v1/clients/" + clientId + "/documents/" + document.id(),
            HttpMethod.PUT,
            new HttpEntity<>(update),
            DocumentResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(document.id());
    assertThat(response.getBody().number()).isEqualTo("DOC-2");
    assertThat(response.getBody().clientId()).isEqualTo(clientId);
  }

  @Test
  void updateDocumentWithInvalidBodyReturns400() {
    Long clientId = createClient("123456789", "ana@example.com");
    DocumentResponse document = createDocument(clientId, "DOC-1", LocalDate.of(2030, 1, 1));
    var update = new UpdateDocumentRequest("", "Identity card", LocalDate.of(2031, 2, 2));

    var response =
        rest.exchange(
            "/api/v1/clients/" + clientId + "/documents/" + document.id(),
            HttpMethod.PUT,
            new HttpEntity<>(update),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode problem =
        assertProblem(
            response.getBody(),
            HttpStatus.BAD_REQUEST,
            "/problems/validation-failed",
            "Validation failed",
            "VALIDATION_FAILED");
    assertValidationError(problem, "number");
  }

  @Test
  void updateDocumentWithDuplicateNumberForSameClientReturns409() {
    Long clientId = createClient("123456789", "ana@example.com");
    createDocument(clientId, "DOC-1", LocalDate.of(2030, 1, 1));
    DocumentResponse second = createDocument(clientId, "DOC-2", LocalDate.of(2031, 1, 1));

    var response =
        rest.exchange(
            "/api/v1/clients/" + clientId + "/documents/" + second.id(),
            HttpMethod.PUT,
            new HttpEntity<>(
                new UpdateDocumentRequest("DOC-1", "Identity card", LocalDate.of(2031, 2, 2))),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertProblem(
        response.getBody(),
        HttpStatus.CONFLICT,
        "/problems/document-number-already-exists",
        "Document number already exists",
        "DOCUMENT_NUMBER_EXISTS");
  }

  @Test
  void updateMissingDocumentReturns404() {
    Long clientId = createClient("123456789", "ana@example.com");

    var response =
        rest.exchange(
            "/api/v1/clients/" + clientId + "/documents/999",
            HttpMethod.PUT,
            new HttpEntity<>(
                new UpdateDocumentRequest("DOC-1", "Passport", LocalDate.of(2030, 1, 1))),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertProblem(
        response.getBody(),
        HttpStatus.NOT_FOUND,
        "/problems/document-not-found",
        "Document not found",
        "DOCUMENT_NOT_FOUND");
  }

  @Test
  void updateDocumentOwnedByDifferentClientReturns404() {
    Long firstClientId = createClient("123456789", "ana@example.com");
    Long secondClientId = createClient("987654321", "bob@example.com");
    DocumentResponse document = createDocument(firstClientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var response =
        rest.exchange(
            "/api/v1/clients/" + secondClientId + "/documents/" + document.id(),
            HttpMethod.PUT,
            new HttpEntity<>(
                new UpdateDocumentRequest("DOC-2", "Identity card", LocalDate.of(2031, 2, 2))),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertProblem(
        response.getBody(),
        HttpStatus.NOT_FOUND,
        "/problems/document-not-found",
        "Document not found",
        "DOCUMENT_NOT_FOUND");
  }

  @Test
  void deleteDocumentReturns204AndRemovesDocument() {
    Long clientId = createClient("123456789", "ana@example.com");
    DocumentResponse document = createDocument(clientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var deleteResponse =
        rest.exchange(
            "/api/v1/clients/" + clientId + "/documents/" + document.id(),
            HttpMethod.DELETE,
            null,
            Void.class);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    var getResponse =
        rest.getForEntity(
            "/api/v1/clients/" + clientId + "/documents/" + document.id(), String.class);

    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void deleteMissingDocumentReturns404() {
    Long clientId = createClient("123456789", "ana@example.com");

    var response =
        rest.exchange(
            "/api/v1/clients/" + clientId + "/documents/999",
            HttpMethod.DELETE,
            null,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertProblem(
        response.getBody(),
        HttpStatus.NOT_FOUND,
        "/problems/document-not-found",
        "Document not found",
        "DOCUMENT_NOT_FOUND");
  }

  @Test
  void deleteDocumentOwnedByDifferentClientReturns404() {
    Long firstClientId = createClient("123456789", "ana@example.com");
    Long secondClientId = createClient("987654321", "bob@example.com");
    DocumentResponse document = createDocument(firstClientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var response =
        rest.exchange(
            "/api/v1/clients/" + secondClientId + "/documents/" + document.id(),
            HttpMethod.DELETE,
            null,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertProblem(
        response.getBody(),
        HttpStatus.NOT_FOUND,
        "/problems/document-not-found",
        "Document not found",
        "DOCUMENT_NOT_FOUND");
  }

  @Test
  void listDocumentsReturnsPagedDocuments() {
    Long clientId = createClient("123456789", "ana@example.com");
    createDocument(clientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var response = rest.getForEntity("/api/v1/clients/" + clientId + "/documents", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode body = parseJson(response.getBody());
    assertThat(body.path("content")).hasSize(1);
    assertThat(body.path("page").asInt()).isZero();
    assertThat(body.path("size").asInt()).isEqualTo(20);
    assertThat(body.path("numberOfElements").asInt()).isEqualTo(1);
    assertThat(body.path("totalElements").asLong()).isEqualTo(1);
    assertThat(body.path("totalPages").asInt()).isEqualTo(1);
    assertThat(body.path("content").get(0).path("number").asText()).isEqualTo("DOC-1");
    assertThat(body.path("content").get(0).path("clientId").asLong()).isEqualTo(clientId);
  }

  @Test
  void listDocumentsCapsLargePageSize() {
    Long clientId = createClient("123456789", "ana@example.com");
    createDocument(clientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var response =
        rest.getForEntity("/api/v1/clients/" + clientId + "/documents?size=500", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode body = parseJson(response.getBody());
    assertThat(body.path("size").asInt()).isEqualTo(100);
  }

  @Test
  void listDocumentsWithUnsupportedSortReturns400() {
    Long clientId = createClient("123456789", "ana@example.com");

    var response =
        rest.getForEntity(
            "/api/v1/clients/" + clientId + "/documents?sort=client.email,asc", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertProblem(
        response.getBody(),
        HttpStatus.BAD_REQUEST,
        "/problems/invalid-sort",
        "Invalid sort",
        "INVALID_SORT");
  }

  @Test
  void listDocumentsForMissingClientReturns404() {
    var response = rest.getForEntity("/api/v1/clients/999/documents", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertProblem(
        response.getBody(),
        HttpStatus.NOT_FOUND,
        "/problems/client-not-found",
        "Client not found",
        "CLIENT_NOT_FOUND");
  }

  @Test
  void listDocumentsWithNonPositiveClientIdReturns400() {
    var response = rest.getForEntity("/api/v1/clients/0/documents", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode problem =
        assertProblem(
            response.getBody(),
            HttpStatus.BAD_REQUEST,
            "/problems/validation-failed",
            "Validation failed",
            "VALIDATION_FAILED");
    assertValidationError(problem, "clientId");
  }

  @Test
  void listAllDocumentsReturnsPagedDocumentsAcrossClients() {
    Long firstClientId = createClient("123456789", "ana@example.com");
    Long secondClientId = createClient("987654321", "bob@example.com");
    createDocument(firstClientId, "DOC-2", LocalDate.of(2031, 1, 1));
    createDocument(secondClientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var response = rest.getForEntity("/api/v1/documents?sort=number,asc", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode body = parseJson(response.getBody());
    assertThat(body.path("content")).hasSize(2);
    assertThat(body.path("page").asInt()).isZero();
    assertThat(body.path("size").asInt()).isEqualTo(20);
    assertThat(body.path("numberOfElements").asInt()).isEqualTo(2);
    assertThat(body.path("totalElements").asLong()).isEqualTo(2);
    assertThat(body.path("totalPages").asInt()).isEqualTo(1);
    assertThat(body.path("content").get(0).path("number").asText()).isEqualTo("DOC-1");
    assertThat(body.path("content").get(0).path("clientId").asLong()).isEqualTo(secondClientId);
    assertThat(body.path("content").get(1).path("number").asText()).isEqualTo("DOC-2");
    assertThat(body.path("content").get(1).path("clientId").asLong()).isEqualTo(firstClientId);
  }

  @Test
  void listAllDocumentsCapsLargePageSize() {
    Long clientId = createClient("123456789", "ana@example.com");
    createDocument(clientId, "DOC-1", LocalDate.of(2030, 1, 1));

    var response = rest.getForEntity("/api/v1/documents?size=500", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode body = parseJson(response.getBody());
    assertThat(body.path("size").asInt()).isEqualTo(100);
  }

  @Test
  void listAllDocumentsWithUnsupportedSortReturns400() {
    var response = rest.getForEntity("/api/v1/documents?sort=client.email,asc", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertProblem(
        response.getBody(),
        HttpStatus.BAD_REQUEST,
        "/problems/invalid-sort",
        "Invalid sort",
        "INVALID_SORT");
  }

  private Long createClient(String taxIdentifier, String email) {
    var response =
        rest.postForEntity(
            "/api/v1/clients",
            new CreateClientRequest("Ana", "Silva", taxIdentifier, email, "+351910000000"),
            ClientResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    return response.getBody().id();
  }

  private DocumentResponse createDocument(Long clientId, String number, LocalDate expirationDate) {
    var response =
        rest.postForEntity(
            "/api/v1/clients/" + clientId + "/documents",
            documentRequest(number, "Passport", expirationDate),
            DocumentResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    return response.getBody();
  }

  private CreateDocumentRequest documentRequest(
      String number, String description, LocalDate expirationDate) {
    return new CreateDocumentRequest(number, description, expirationDate);
  }
}
