package humanit.client;

import static humanit.helper.ApiAssertions.assertProblem;
import static humanit.helper.ApiAssertions.assertValidationError;
import static humanit.helper.ApiAssertions.parseJson;
import static humanit.helper.AuthTestSupport.authenticate;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.databind.JsonNode;

import humanit.client.dto.ClientResponse;
import humanit.client.dto.CreateClientRequest;
import humanit.client.dto.UpdateClientRequest;
import humanit.document.dto.CreateDocumentRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
                "DELETE FROM app_users",
                "DELETE FROM documents",
                "DELETE FROM clients"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ClientApiIT {
        @Autowired
        TestRestTemplate rest;

        @BeforeEach
        void authenticateRequests() {
                authenticate(rest);
        }

        @Test
        void createClientReturns201AndLocationHeader() {
                var request = clientRequest(
                                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000");

                var response = rest.postForEntity(
                                "/api/v1/clients",
                                request,
                                ClientResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getHeaders().getLocation()).isNotNull();
                assertThat(response.getHeaders().getLocation().toString())
                                .endsWith("/api/v1/clients/" + response.getBody().id());
                assertThat(response.getBody().id()).isNotNull();
                assertThat(response.getBody().email()).isEqualTo("ana@example.com");
        }

        @Test
        void createClientWithDocumentsReturns201AndPersistsDocuments() {
                var request = new CreateClientRequest(
                                "Ana",
                                "Silva",
                                "TAX-1",
                                "ana@example.com",
                                "910000000",
                                List.of(new CreateDocumentRequest(
                                                "DOC-1",
                                                "Passport",
                                                LocalDate.of(2030, 1, 1))));

                var response = rest.postForEntity(
                                "/api/v1/clients",
                                request,
                                ClientResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().documents()).hasSize(1);
                assertThat(response.getBody().documents().getFirst().id()).isNotNull();
                assertThat(response.getBody().documents().getFirst().number()).isEqualTo("DOC-1");
                assertThat(response.getBody().documents().getFirst().clientId()).isEqualTo(response.getBody().id());
        }

        @Test
        void createClientWithInvalidDocumentReturns400() {
                var request = new CreateClientRequest(
                                "Ana",
                                "Silva",
                                "TAX-1",
                                "ana@example.com",
                                "910000000",
                                List.of(new CreateDocumentRequest(
                                                "",
                                                "Passport",
                                                LocalDate.of(2030, 1, 1))));

                var response = rest.postForEntity(
                                "/api/v1/clients",
                                request,
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                JsonNode problem = assertProblem(
                                response.getBody(),
                                HttpStatus.BAD_REQUEST,
                                "/problems/validation-failed",
                                "Validation failed",
                                "VALIDATION_FAILED");
                assertValidationError(problem, "documents[0].number");
        }

        @Test
        void createClientThenFetchClientById() {
                ClientResponse created = createClient(validClientRequest());

                var response = rest.getForEntity(
                                "/api/v1/clients/" + created.id(),
                                ClientResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().id()).isEqualTo(created.id());
                assertThat(response.getBody().email()).isEqualTo("ana@example.com");
        }

        @Test
        void getClientByIdReturnsDocuments() {
                ClientResponse created = createClient(validClientRequest());
                var documentRequest = new CreateDocumentRequest(
                                "DOC-1", "Passport", LocalDate.of(2030, 1, 1));

                var documentResponse = rest.postForEntity(
                                "/api/v1/clients/" + created.id() + "/documents",
                                documentRequest,
                                String.class);

                assertThat(documentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

                var response = rest.getForEntity(
                                "/api/v1/clients/" + created.id(),
                                ClientResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().documents()).hasSize(1);
                assertThat(response.getBody().documents().getFirst().number()).isEqualTo("DOC-1");
                assertThat(response.getBody().documents().getFirst().description()).isEqualTo("Passport");
                assertThat(response.getBody().documents().getFirst().expirationDate())
                                .isEqualTo(LocalDate.of(2030, 1, 1));
                assertThat(response.getBody().documents().getFirst().clientId()).isEqualTo(created.id());
        }

        @Test
        void createClientWithInvalidBodyReturns400() {
                var request = clientRequest(
                                "Ana", "Silva", "TAX-1", "bad-email", "910000000");

                var response = rest.postForEntity(
                                "/api/v1/clients",
                                request,
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                JsonNode problem = assertProblem(
                                response.getBody(),
                                HttpStatus.BAD_REQUEST,
                                "/problems/validation-failed",
                                "Validation failed",
                                "VALIDATION_FAILED");
                assertValidationError(problem, "email");
        }

        @Test
        void createClientWithMalformedJsonReturns400() {
                var response = rest.postForEntity(
                                "/api/v1/clients",
                                jsonEntity("{"),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertProblem(
                                response.getBody(),
                                HttpStatus.BAD_REQUEST,
                                "/problems/malformed-request",
                                "Malformed request",
                                "MALFORMED_REQUEST");
        }

        @Test
        void createClientWithMissingRequiredFieldReturns400() {
                var request = clientRequest(
                                "", "Silva", "TAX-1", "ana@example.com", "910000000");

                var response = rest.postForEntity(
                                "/api/v1/clients",
                                request,
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                JsonNode problem = assertProblem(
                                response.getBody(),
                                HttpStatus.BAD_REQUEST,
                                "/problems/validation-failed",
                                "Validation failed",
                                "VALIDATION_FAILED");
                assertValidationError(problem, "firstName");
        }

        @Test
        void createClientWithDuplicateEmailReturns409() {
                createClient(validClientRequest());

                var duplicate = clientRequest(
                                "Bob", "Costa", "TAX-2", "ana@example.com", "920000000");

                var response = rest.postForEntity(
                                "/api/v1/clients",
                                duplicate,
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertProblem(
                                response.getBody(),
                                HttpStatus.CONFLICT,
                                "/problems/client-email-already-exists",
                                "Client email already exists",
                                "CLIENT_EMAIL_EXISTS");
        }

        @Test
        void createClientWithDuplicateTaxIdentifierReturns409() {
                createClient(validClientRequest());

                var duplicate = clientRequest(
                                "Bob", "Costa", "TAX-1", "bob@example.com", "920000000");

                var response = rest.postForEntity(
                                "/api/v1/clients",
                                duplicate,
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertProblem(
                                response.getBody(),
                                HttpStatus.CONFLICT,
                                "/problems/client-tax-identifier-already-exists",
                                "Client tax identifier already exists",
                                "CLIENT_TAX_IDENTIFIER_EXISTS");
        }

        @Test
        void getMissingClientReturns404() {
                var response = rest.getForEntity(
                                "/api/v1/clients/999",
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
        void getClientWithNonNumericIdReturns400() {
                var response = rest.getForEntity(
                                "/api/v1/clients/not-a-number",
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertProblem(
                                response.getBody(),
                                HttpStatus.BAD_REQUEST,
                                "/problems/malformed-request",
                                "Malformed request",
                                "MALFORMED_REQUEST");
        }

        @Test
        void getClientWithNonPositiveIdReturns400() {
                var response = rest.getForEntity(
                                "/api/v1/clients/-1",
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                JsonNode problem = assertProblem(
                                response.getBody(),
                                HttpStatus.BAD_REQUEST,
                                "/problems/validation-failed",
                                "Validation failed",
                                "VALIDATION_FAILED");
                assertValidationError(problem, "id");
        }

        @Test
        void updateClientReturns200AndUpdatedBody() {
                ClientResponse created = createClient(validClientRequest());
                var update = clientUpdateRequest(
                                "Bea", "Costa", "TAX-2", "bea@example.com", "920000000");

                var response = rest.exchange(
                                "/api/v1/clients/" + created.id(),
                                HttpMethod.PUT,
                                new HttpEntity<>(update),
                                ClientResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().id()).isEqualTo(created.id());
                assertThat(response.getBody().email()).isEqualTo("bea@example.com");
        }

        @Test
        void updateMissingClientReturns404() {
                var update = clientUpdateRequest(
                                "Bea", "Costa", "TAX-2", "bea@example.com", "920000000");

                var response = rest.exchange(
                                "/api/v1/clients/999",
                                HttpMethod.PUT,
                                new HttpEntity<>(update),
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
        void updateClientWithInvalidBodyReturns400() {
                ClientResponse created = createClient(validClientRequest());
                var update = clientUpdateRequest(
                                "Bea", "Costa", "TAX-2", "bad-email", "920000000");

                var response = rest.exchange(
                                "/api/v1/clients/" + created.id(),
                                HttpMethod.PUT,
                                new HttpEntity<>(update),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                JsonNode problem = assertProblem(
                                response.getBody(),
                                HttpStatus.BAD_REQUEST,
                                "/problems/validation-failed",
                                "Validation failed",
                                "VALIDATION_FAILED");
                assertValidationError(problem, "email");
        }

        @Test
        void updateClientWithDuplicateEmailReturns409() {
                createClient(validClientRequest());
                ClientResponse second = createClient(clientRequest(
                                "Bob", "Costa", "TAX-2", "bob@example.com", "920000000"));

                var response = rest.exchange(
                                "/api/v1/clients/" + second.id(),
                                HttpMethod.PUT,
                                new HttpEntity<>(clientUpdateRequest(
                                                "Bob", "Costa", "TAX-2", "ana@example.com", "920000000")),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertProblem(
                                response.getBody(),
                                HttpStatus.CONFLICT,
                                "/problems/client-email-already-exists",
                                "Client email already exists",
                                "CLIENT_EMAIL_EXISTS");
        }

        @Test
        void updateClientWithDuplicateTaxIdentifierReturns409() {
                createClient(validClientRequest());
                ClientResponse second = createClient(clientRequest(
                                "Bob", "Costa", "TAX-2", "bob@example.com", "920000000"));

                var response = rest.exchange(
                                "/api/v1/clients/" + second.id(),
                                HttpMethod.PUT,
                                new HttpEntity<>(clientUpdateRequest(
                                                "Bob", "Costa", "TAX-1", "bob@example.com", "920000000")),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertProblem(
                                response.getBody(),
                                HttpStatus.CONFLICT,
                                "/problems/client-tax-identifier-already-exists",
                                "Client tax identifier already exists",
                                "CLIENT_TAX_IDENTIFIER_EXISTS");
        }

        @Test
        void deleteClientReturns204AndRemovesClient() {
                ClientResponse created = createClient(validClientRequest());

                var deleteResponse = rest.exchange(
                                "/api/v1/clients/" + created.id(),
                                HttpMethod.DELETE,
                                null,
                                Void.class);

                assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

                var getResponse = rest.getForEntity(
                                "/api/v1/clients/" + created.id(),
                                String.class);

                assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void deleteMissingClientReturns404() {
                var response = rest.exchange(
                                "/api/v1/clients/999",
                                HttpMethod.DELETE,
                                null,
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
        void listClientsReturnsPagedClients() {
                createClient(validClientRequest());

                var response = rest.getForEntity(
                                "/api/v1/clients",
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                JsonNode body = parseJson(response.getBody());
                assertThat(body.path("content")).hasSize(1);
                assertThat(body.path("includeDocuments").asBoolean()).isFalse();
                assertThat(body.path("page").asInt()).isZero();
                assertThat(body.path("size").asInt()).isEqualTo(20);
                assertThat(body.path("numberOfElements").asInt()).isEqualTo(1);
                assertThat(body.path("totalElements").asLong()).isEqualTo(1);
                assertThat(body.path("totalPages").asInt()).isEqualTo(1);
                assertThat(body.path("content").get(0).path("email").asText()).isEqualTo("ana@example.com");
                assertThat(body.path("content").get(0).path("documentCount").asLong()).isZero();
                assertThat(body.path("content").get(0).has("documents")).isFalse();
        }

        @Test
        void listClientsWithInvalidIncludeDocumentsQueryParamReturns400() {
                var response = rest.getForEntity(
                                "/api/v1/clients?includeDocuments=not-a-boolean",
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertProblem(
                                response.getBody(),
                                HttpStatus.BAD_REQUEST,
                                "/problems/malformed-request",
                                "Malformed request",
                                "MALFORMED_REQUEST");
        }

        @Test
        void listClientsWithIncludeDocumentsReturnsPagedClientResponses() {
                ClientResponse first = createClient(validClientRequest());
                ClientResponse second = createClient(clientRequest(
                                "Bob", "Costa", "TAX-2", "bob@example.com", "920000000"));
                createDocument(first.id(), "DOC-1");
                createDocument(second.id(), "DOC-2");

                var response = rest.getForEntity(
                                "/api/v1/clients?includeDocuments=true&size=1&sort=lastName,asc",
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                JsonNode body = parseJson(response.getBody());
                assertThat(body.path("content")).hasSize(1);
                assertThat(body.path("includeDocuments").asBoolean()).isTrue();
                assertThat(body.path("page").asInt()).isZero();
                assertThat(body.path("size").asInt()).isEqualTo(1);
                assertThat(body.path("numberOfElements").asInt()).isEqualTo(1);
                assertThat(body.path("totalElements").asLong()).isEqualTo(2);
                assertThat(body.path("totalPages").asInt()).isEqualTo(2);

                JsonNode client = body.path("content").get(0);
                assertThat(client.path("email").asText()).isEqualTo("bob@example.com");
                assertThat(client.has("documentCount")).isFalse();
                assertThat(client.path("documents")).hasSize(1);
                assertThat(client.path("documents").get(0).path("number").asText()).isEqualTo("DOC-2");
        }

        private ClientResponse createClient(CreateClientRequest request) {
                var response = rest.postForEntity(
                                "/api/v1/clients",
                                request,
                                ClientResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                return response.getBody();
        }

        private void createDocument(Long clientId, String number) {
                var request = new CreateDocumentRequest(
                                number, "Passport", LocalDate.of(2030, 1, 1));

                var response = rest.postForEntity(
                                "/api/v1/clients/" + clientId + "/documents",
                                request,
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        private HttpEntity<String> jsonEntity(String json) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                return new HttpEntity<>(json, headers);
        }

        private CreateClientRequest validClientRequest() {
                return clientRequest("Ana", "Silva", "TAX-1", "ana@example.com", "910000000");
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
