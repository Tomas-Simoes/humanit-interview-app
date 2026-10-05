# Humanit Interview API - Implementation Plan

## 2. Assumptions

The requirements leave a few areas open. These assumptions keep the solution focused without over-engineering it.

| Area                           | Assumption                                                                                                                                                        |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| API base path                  | Use `/api/v1` for all application endpoints.                                                                                                                      |
| Document CRUD                  | Because the objective mentions managing Clients and associated Documents, nested Document endpoints will be included in addition to the minimum Client endpoints. |
| Client creation with documents | `POST /api/v1/clients` may include documents and will persist the Client and all Documents atomically.                                                            |
| Client update semantics        | `PUT /api/v1/clients/{id}` replaces the Client aggregate, including the submitted document collection. For document-only changes, use nested Document endpoints.  |
| Authentication user source     | No user-management requirements are provided. Implement JWT login using a configured application user, not a full user registration/domain model.                 |
| Database                       | Use H2 in-memory database for development and tests.                                                                                                              |
| Schema management              | Use Flyway migrations for explicit schema management, with Hibernate validating the schema.                                                                       |
| IDs                            | Use generated numeric IDs.                                                                                                                                        |
| Dates                          | `expirationDate` is required but not forced to be in the future, because expired documents may still need to be stored for audit or business purposes.            |
| Deleting a Client              | Deleting a Client also deletes its Documents using cascade and orphan removal.                                                                                    |
| API list responses             | `GET /clients` returns paginated Client summaries; `GET /clients/{id}` returns full Client details including Documents.                                           |

## 4. Architecture

Use a layered architecture with clear boundaries:

```text
Controller layer
-> receives HTTP requests
-> validates DTOs
-> delegates to services
-> returns HTTP responses
Service layer
-> owns business/application logic
-> defines transactional boundaries
-> coordinates repositories and mappers
-> enforces aggregate-level rules
Persistence layer
-> JPA entities
-> Spring Data repositories
-> database constraints and relationships
Infrastructure layer
-> security configuration
-> JWT generation and validation
-> OpenAPI configuration
-> exception handling
-> logging configuration
```

### 4.1 Package Structure

Recommended package root:

```text
com.humanit.interview
```

Recommended package layout:

```text
com.humanit.interview
HumanitInterviewApiApplication

auth
AuthController
AuthService
JwtService
LoginRequest
LoginResponse

client
ClientController
ClientService
ClientRepository
Client
ClientMapper

dto
CreateClientRequest
UpdateClientRequest
UpdateClientDocumentRequest
ClientResponse
ClientSummaryResponse

document
ClientDocumentController
DocumentService
DocumentRepository
Document
DocumentMapper
dto
CreateDocumentRequest
UpdateDocumentRequest
DocumentResponse

common
error
ApiExceptionHandler
ErrorResponse
ValidationError
ResourceNotFoundException
ConflictException
pagination
PageResponse

config
SecurityConfig
OpenApiConfig
WebConfig
```

### 4.2 Architectural Principles

- Controllers must not contain business logic.
- Services own transactions and business decisions.
- Repositories are persistence-only abstractions.
- DTOs are used for all request and response payloads.
- JPA entities are never exposed directly through the API.
- Mappers keep conversion logic isolated.
- Validation is declared on request DTOs where possible.
- Cross-cutting concerns such as errors, security, and documentation are centralized.
- The Client and its Documents are treated as an aggregate for create/update operations.
-

## 5. Domain Model

### 5.1 Client Entity

Represents a person/customer managed by the API.

| Field           | Type             | Constraints                                             |
| --------------- | ---------------- | ------------------------------------------------------- |
| `id`            | `Long`           | Primary key, generated, not exposed in create requests. |
| `firstName`     | `String`         | Required, max 100 characters.                           |
| `lastName`      | `String`         | Required, max 100 characters.                           |
| `taxIdentifier` | `String`         | Required, max 50 characters, unique.                    |
| `email`         | `String`         | Required, valid email, max 255 characters, unique.      |
| `phoneNumber`   | `String`         | Required, max 30 characters.                            |
| `documents`     | `List<Document>` | One-to-many, cascade all, orphan removal.               |

Database constraints:

- Primary key on `id`.
- Unique constraint on `tax_identifier`.
- Unique constraint on `email`.
- Non-null constraints for required fields.
  Relationship:
- One Client can have many Documents.
- Client owns the Document lifecycle.
- Deleting a Client deletes its Documents.

### 5.2 Document Entity

Represents a document associated with one Client.

| Field            | Type        | Constraints                                             |
| ---------------- | ----------- | ------------------------------------------------------- |
| `id`             | `Long`      | Primary key, generated, not exposed in create requests. |
| `number`         | `String`    | Required, max 100 characters.                           |
| `description`    | `String`    | Optional, max 500 characters.                           |
| `expirationDate` | `LocalDate` | Required.                                               |
| `client`         | `Client`    | Required many-to-one relationship.                      |

Database constraints:

- Primary key on `id`.
- Foreign key from `documents.client_id` to `clients.id`.
- Non-null constraints for `number`, `expiration_date`, and `client_id`.
- Unique constraint on `(client_id, number)` so a Client cannot have duplicate document numbers.
  Relationship:
- Many Documents belong to one Client.
- A Document cannot exist without a Client.
- Document endpoints are nested under a Client to make ownership explicit.

## 6. DTO Model

### 6.1 Client Request DTOs

#### CreateClientRequest

| Field           | Type                          | Required | Validation                       |
| --------------- | ----------------------------- | -------- | -------------------------------- |
| `firstName`     | `String`                      | Yes      | Not blank, max 100.              |
| `lastName`      | `String`                      | Yes      | Not blank, max 100.              |
| `taxIdentifier` | `String`                      | Yes      | Not blank, max 50.               |
| `email`         | `String`                      | Yes      | Not blank, valid email, max 255. |
| `phoneNumber`   | `String`                      | Yes      | Not blank, max 30.               |
| `documents`     | `List<CreateDocumentRequest>` | No       | Valid nested DTOs.               |

#### UpdateClientRequest

| Field           | Type                                | Required | Validation                                                               |
| --------------- | ----------------------------------- | -------- | ------------------------------------------------------------------------ |
| `firstName`     | `String`                            | Yes      | Not blank, max 100.                                                      |
| `lastName`      | `String`                            | Yes      | Not blank, max 100.                                                      |
| `taxIdentifier` | `String`                            | Yes      | Not blank, max 50.                                                       |
| `email`         | `String`                            | Yes      | Not blank, valid email, max 255.                                         |
| `phoneNumber`   | `String`                            | Yes      | Not blank, max 30.                                                       |
| `documents`     | `List<UpdateClientDocumentRequest>` | Yes      | Valid nested DTOs, can be empty. Missing existing documents are removed. |

#### UpdateClientDocumentRequest

Used only when replacing the full Client aggregate through `PUT /api/v1/clients/{id}`.

| Field            | Type        | Required | Validation                                                                                       |
| ---------------- | ----------- | -------- | ------------------------------------------------------------------------------------------------ |
| `id`             | `Long`      | No       | If present, updates an existing Document owned by the Client. If absent, creates a new Document. |
| `number`         | `String`    | Yes      | Not blank, max 100.                                                                              |
| `description`    | `String`    | No       | Max 500.                                                                                         |
| `expirationDate` | `LocalDate` | Yes      | Not null, ISO date format.                                                                       |

### 6.2 Document Request DTOs

#### CreateDocumentRequest

| Field            | Type        | Required | Validation                 |
| ---------------- | ----------- | -------- | -------------------------- |
| `number`         | `String`    | Yes      | Not blank, max 100.        |
| `description`    | `String`    | No       | Max 500.                   |
| `expirationDate` | `LocalDate` | Yes      | Not null, ISO date format. |

#### UpdateDocumentRequest

| Field            | Type        | Required | Validation                 |
| ---------------- | ----------- | -------- | -------------------------- |
| `number`         | `String`    | Yes      | Not blank, max 100.        |
| `description`    | `String`    | No       | Max 500.                   |
| `expirationDate` | `LocalDate` | Yes      | Not null, ISO date format. |

### 6.3 Response DTOs

#### ClientResponse

Returned by create, retrieve by ID, and update operations.

| Field           | Type                     |
| --------------- | ------------------------ |
| `id`            | `Long`                   |
| `firstName`     | `String`                 |
| `lastName`      | `String`                 |
| `taxIdentifier` | `String`                 |
| `email`         | `String`                 |
| `phoneNumber`   | `String`                 |
| `documents`     | `List<DocumentResponse>` |

#### ClientSummaryResponse

Returned by paginated Client list operations.

| Field           | Type      |
| --------------- | --------- |
| `id`            | `Long`    |
| `firstName`     | `String`  |
| `lastName`      | `String`  |
| `taxIdentifier` | `String`  |
| `email`         | `String`  |
| `phoneNumber`   | `String`  |
| `documentCount` | `Integer` |

#### DocumentResponse

| Field            | Type        |
| ---------------- | ----------- |
| `id`             | `Long`      |
| `number`         | `String`    |
| `description`    | `String`    |
| `expirationDate` | `LocalDate` |

#### PageResponse

Used for paginated collection responses.

| Field           | Type      |
| --------------- | --------- |
| `content`       | `List<T>` |
| `page`          | `Integer` |
| `size`          | `Integer` |
| `totalElements` | `Long`    |
| `totalPages`    | `Integer` |
| `first`         | `Boolean` |
| `last`          | `Boolean` |

## 7. API Structure

All endpoints except login, OpenAPI, and the H2 console require a valid JWT bearer token.
Base path:

```text
/api/v1
```

### 7.1 Authentication API

#### POST `/api/v1/auth/login`

Authenticates a configured application user and returns a JWT access token.
Request:

```json
{
  "username": "admin",
  "password": "password"
}
```

Response `200 OK`:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Status codes:

| Status             | Meaning                                          |
| ------------------ | ------------------------------------------------ |
| `200 OK`           | Credentials are valid and a token was generated. |
| `400 Bad Request`  | Request body is malformed or fails validation.   |
| `401 Unauthorized` | Credentials are invalid.                         |

### 7.2 Client API

#### POST `/api/v1/clients`

Creates a Client and optionally its Documents in one transaction.
Request:

```json
{
  "firstName": "Ana",
  "lastName": "Silva",
  "taxIdentifier": "PT123456789",
  "email": "ana.silva@example.com",
  "phoneNumber": "+351912345678",
  "documents": [
    {
      "number": "ID-123",
      "description": "Citizen card",
      "expirationDate": "2030-12-31"
    }
  ]
}
```

Response `201 Created`:

- `Location` header: `/api/v1/clients/{id}`
- Body: `ClientResponse`
  Status codes:

| Status             | Meaning                                                                       |
| ------------------ | ----------------------------------------------------------------------------- |
| `201 Created`      | Client created successfully.                                                  |
| `400 Bad Request`  | Validation failed.                                                            |
| `401 Unauthorized` | Missing or invalid JWT.                                                       |
| `409 Conflict`     | Duplicate email, tax identifier, or duplicate document number for the Client. |

#### GET `/api/v1/clients`

Retrieves paginated Clients.
Query parameters:

| Parameter | Default        | Notes                                         |
| --------- | -------------- | --------------------------------------------- |
| `page`    | `0`            | Zero-based page index.                        |
| `size`    | `20`           | Maximum should be capped, for example at 100. |
| `sort`    | `lastName,asc` | Allow only safe sortable fields.              |

Response `200 OK`:

```json
{
  "content": [
    {
      "id": 1,
      "firstName": "Ana",
      "lastName": "Silva",
      "taxIdentifier": "PT123456789",
      "email": "ana.silva@example.com",
      "phoneNumber": "+351912345678",
      "documentCount": 1
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

Status codes:

| Status             | Meaning                                  |
| ------------------ | ---------------------------------------- |
| `200 OK`           | Clients returned successfully.           |
| `400 Bad Request`  | Invalid pagination or sorting parameter. |
| `401 Unauthorized` | Missing or invalid JWT.                  |

#### GET `/api/v1/clients/{id}`

Retrieves one Client by ID, including Documents.
Response `200 OK`: `ClientResponse`
Status codes:

| Status             | Meaning                 |
| ------------------ | ----------------------- |
| `200 OK`           | Client found.           |
| `401 Unauthorized` | Missing or invalid JWT. |
| `404 Not Found`    | Client does not exist.  |

#### PUT `/api/v1/clients/{id}`

Replaces a Client aggregate.
Request:

```json
{
  "firstName": "Ana",
  "lastName": "Santos",
  "taxIdentifier": "PT123456789",
  "email": "ana.santos@example.com",
  "phoneNumber": "+351912345678",
  "documents": [
    {
      "id": 10,
      "number": "ID-123",
      "description": "Citizen card",
      "expirationDate": "2030-12-31"
    }
  ]
}
```

Response `200 OK`: `ClientResponse`
Status codes:

| Status             | Meaning                                              |
| ------------------ | ---------------------------------------------------- |
| `200 OK`           | Client updated successfully.                         |
| `400 Bad Request`  | Validation failed.                                   |
| `401 Unauthorized` | Missing or invalid JWT.                              |
| `404 Not Found`    | Client does not exist.                               |
| `409 Conflict`     | Duplicate email, tax identifier, or document number. |

#### DELETE `/api/v1/clients/{id}`

Deletes a Client and its Documents.
Response:

- `204 No Content`
  Status codes:

| Status             | Meaning                      |
| ------------------ | ---------------------------- |
| `204 No Content`   | Client deleted successfully. |
| `401 Unauthorized` | Missing or invalid JWT.      |
| `404 Not Found`    | Client does not exist.       |

### 7.3 Document API

Documents are nested under Clients to make ownership explicit.

#### POST `/api/v1/clients/{clientId}/documents`

Creates a Document for an existing Client.
Request: `CreateDocumentRequest`
Response `201 Created`:

- `Location` header: `/api/v1/clients/{clientId}/documents/{documentId}`
- Body: `DocumentResponse`
  Status codes:

| Status             | Meaning                                         |
| ------------------ | ----------------------------------------------- |
| `201 Created`      | Document created successfully.                  |
| `400 Bad Request`  | Validation failed.                              |
| `401 Unauthorized` | Missing or invalid JWT.                         |
| `404 Not Found`    | Client does not exist.                          |
| `409 Conflict`     | Document number already exists for this Client. |

#### GET `/api/v1/clients/{clientId}/documents`

Retrieves Documents for a Client.
Response `200 OK`:

```json
[
  {
    "id": 10,
    "number": "ID-123",
    "description": "Citizen card",
    "expirationDate": "2030-12-31"
  }
]
```

Status codes:

| Status             | Meaning                          |
| ------------------ | -------------------------------- |
| `200 OK`           | Documents returned successfully. |
| `401 Unauthorized` | Missing or invalid JWT.          |
| `404 Not Found`    | Client does not exist.           |

#### GET `/api/v1/clients/{clientId}/documents/{documentId}`

Retrieves a single Document belonging to a Client.
Response `200 OK`: `DocumentResponse`
Status codes:

| Status             | Meaning                            |
| ------------------ | ---------------------------------- |
| `200 OK`           | Document found.                    |
| `401 Unauthorized` | Missing or invalid JWT.            |
| `404 Not Found`    | Client or Document does not exist. |

#### PUT `/api/v1/clients/{clientId}/documents/{documentId}`

Replaces a Document belonging to a Client.
Request: `UpdateDocumentRequest`
Response `200 OK`: `DocumentResponse`
Status codes:

| Status             | Meaning                                         |
| ------------------ | ----------------------------------------------- |
| `200 OK`           | Document updated successfully.                  |
| `400 Bad Request`  | Validation failed.                              |
| `401 Unauthorized` | Missing or invalid JWT.                         |
| `404 Not Found`    | Client or Document does not exist.              |
| `409 Conflict`     | Document number already exists for this Client. |

#### DELETE `/api/v1/clients/{clientId}/documents/{documentId}`

Deletes a Document belonging to a Client.
Response:

- `204 No Content`
  Status codes:

| Status             | Meaning                            |
| ------------------ | ---------------------------------- |
| `204 No Content`   | Document deleted successfully.     |
| `401 Unauthorized` | Missing or invalid JWT.            |
| `404 Not Found`    | Client or Document does not exist. |

## 8. Error Handling

Use one global exception handler for consistent API errors.
Recommended response format:

```json
{
  "timestamp": "2026-10-05T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/clients",
  "violations": [
    {
      "field": "email",
      "message": "must be a valid email address"
    }
  ]
}
```

Error mapping:

| Exception or condition      | Status                      |
| --------------------------- | --------------------------- |
| Validation failure          | `400 Bad Request`           |
| Malformed JSON              | `400 Bad Request`           |
| Invalid pagination/sort     | `400 Bad Request`           |
| Invalid login credentials   | `401 Unauthorized`          |
| Missing/invalid JWT         | `401 Unauthorized`          |
| Authenticated but forbidden | `403 Forbidden`             |
| Resource not found          | `404 Not Found`             |
| Duplicate unique value      | `409 Conflict`              |
| Unexpected server error     | `500 Internal Server Error` |

## 9. Transaction Strategy

Transactional boundaries belong in the service layer.
Methods that must be transactional:

- `ClientService.createClient`
- `ClientService.updateClient`
- `ClientService.deleteClient`
- `DocumentService.createDocument`
- `DocumentService.updateDocument`
- `DocumentService.deleteDocument`
  Read-only transactions:
- `ClientService.getClient`
- `ClientService.listClients`
- `DocumentService.listDocuments`
- `DocumentService.getDocument`
  Atomicity rules:
- Creating a Client with Documents succeeds or fails as one unit.
- Updating a Client aggregate succeeds or fails as one unit.
- If any Document is invalid or violates constraints, the Client change is rolled back.
- Document changes are scoped to the owning Client.
- Database uniqueness constraints are the final guard against race conditions.

## 10. Controller Responsibilities

### 10.1 AuthController

Responsibilities:

- Accept login requests.
- Validate username/password payload shape.
- Delegate authentication to `AuthService`.
- Return JWT response.
  Should not:
- Generate tokens directly.
- Know password hashing details.
- Contain Client or Document logic.

### 10.2 ClientController

Responsibilities:

- Handle `/api/v1/clients` endpoints.
- Validate request DTOs.
- Accept pagination and sorting parameters.
- Delegate all business work to `ClientService`.
- Return `201`, `200`, or `204` responses with correct headers.
  Should not:
- Access repositories directly.
- Perform transaction management.
- Convert JPA entities inline.

### 10.3 ClientDocumentController

Responsibilities:

- Handle `/api/v1/clients/{clientId}/documents` endpoints.
- Ensure document routes are always scoped to a Client ID.
- Validate request DTOs.
- Delegate work to `DocumentService`.
  Should not:
- Accept standalone Document routes unless a future requirement asks for them.
- Expose Documents without confirming Client ownership.

## 11. Service Responsibilities

### 11.1 AuthService

Responsibilities:

- Authenticate configured user credentials.
- Delegate token generation to `JwtService`.
- Return login response data.

### 11.2 JwtService

Responsibilities:

- Generate JWT tokens.
- Validate token signature and expiration.
- Extract username and authorities.
- Hide JWT library details from the rest of the application.

### 11.3 ClientService

Responsibilities:

- Create, retrieve, update, list, and delete Clients.
- Own Client aggregate transactions.
- Check for duplicate email and tax identifier before save where useful.
- Rely on database constraints as final consistency enforcement.
- Map between Client DTOs and Client entities using `ClientMapper`.
- Load Documents with Client details where required.

### 11.4 DocumentService

Responsibilities:

- Create, retrieve, update, list, and delete Documents for a Client.
- Ensure every Document operation is scoped to the owning Client.
- Prevent duplicate document numbers within the same Client.
- Map between Document DTOs and Document entities using `DocumentMapper`.

## 12. Repository Responsibilities

### 12.1 ClientRepository

Responsibilities:

- Provide persistence operations for Clients.
- Support lookup by ID.
- Support duplicate checks:
- exists by email.
- exists by tax identifier.
- exists by email excluding current ID.
- exists by tax identifier excluding current ID.
- Support paginated list retrieval.

### 12.2 DocumentRepository

Responsibilities:

- Provide persistence operations for Documents.
- Support lookup by `clientId` and `documentId`.
- Support listing Documents by `clientId`.
- Support duplicate checks by `clientId` and document number.

## 13. Security Design

JWT authentication is required because optional features are considered part of implementation.

### 13.1 Public Endpoints

- `POST /api/v1/auth/login`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/h2-console/**` in local/dev profile only

### 13.2 Secured Endpoints

- All `/api/v1/clients/**` endpoints.

### 13.3 JWT Behavior

- Login accepts configured username/password.
- On success, API returns a signed JWT.
- Client sends token as `Authorization: Bearer <token>`.
- Security filter validates token before controller execution.
- Invalid, missing, or expired tokens return `401 Unauthorized`.

### 13.4 Configuration

Use environment-configurable values:

| Property                              | Purpose                                            |
| ------------------------------------- | -------------------------------------------------- |
| `app.security.jwt.secret`             | Token signing secret.                              |
| `app.security.jwt.expiration-seconds` | Token lifetime.                                    |
| `app.security.user.username`          | Configured login username.                         |
| `app.security.user.password`          | Configured login password, preferably BCrypt hash. |

## 14. Database and Migration Design

Use Flyway migration scripts:

```text
src/main/resources/db/migration
V1__create_clients_and_documents.sql
```

Tables:

- `clients`
- `documents`
  Recommended JPA settings:
- `spring.jpa.hibernate.ddl-auto=validate`
- `spring.flyway.enabled=true`
- H2 JDBC URL for local/test profiles.
  Why Flyway:
- Keeps schema explicit and reviewable.
- Avoids hidden schema behavior.
- Still satisfies the requirement to automatically create/manage the schema.

## 15. OpenAPI Documentation

Use springdoc-openapi.
Document:

- Auth endpoint.
- Client endpoints.
- Document endpoints.
- Request DTOs.
- Response DTOs.
- Validation rules.
- Error response shape.
- JWT bearer security scheme.
  Available docs:
- `/v3/api-docs`
- `/swagger-ui.html`

## 16. Logging Strategy

Use SLF4J through Spring Boot.
Log:

- Application startup.
- Login success/failure without logging passwords or tokens.
- Client created, updated, deleted by ID.
- Document created, updated, deleted by ID and Client ID.
- Unexpected errors in global exception handling.
  Do not log:
- Passwords.
- JWT tokens.
- Full request bodies containing personal data.
- Sensitive headers.

## 17. Testing Strategy

Tests must be executable through Maven.
Primary command:

```text
mvn test
```

### 17.1 Integration Tests

Use Spring Boot integration tests with MockMvc or TestRestTemplate.
Cover happy path for every endpoint:

- `POST /api/v1/auth/login`
- `POST /api/v1/clients`
- `GET /api/v1/clients`
- `GET /api/v1/clients/{id}`
- `PUT /api/v1/clients/{id}`
- `DELETE /api/v1/clients/{id}`
- `POST /api/v1/clients/{clientId}/documents`
- `GET /api/v1/clients/{clientId}/documents`
- `GET /api/v1/clients/{clientId}/documents/{documentId}`
- `PUT /api/v1/clients/{clientId}/documents/{documentId}`
- `DELETE /api/v1/clients/{clientId}/documents/{documentId}`

### 17.2 Validation and Error Tests

Add tests for:

- Invalid email returns `400`.
- Missing required Client fields return `400`.
- Missing required Document fields return `400`.
- Duplicate email returns `409`.
- Duplicate tax identifier returns `409`.
- Duplicate document number for same Client returns `409`.
- Unknown Client ID returns `404`.
- Unknown Document ID returns `404`.
- Missing token returns `401`.
- Invalid token returns `401`.

### 17.3 Transactional Tests

Add tests for:

- Creating a Client with one invalid Document rolls back the Client creation.
- Updating a Client with invalid duplicate Documents rolls back all changes.
- Deleting a Client removes its Documents.

### 17.4 Service Tests

Add focused service tests where useful:

- Duplicate detection.
- Client-to-document ownership checks.
- Mapper behavior.

## 18. Docker Plan

Provide a multi-stage Dockerfile:

- Build stage uses Maven with JDK 21.
- Runtime stage uses JRE 21.
- Runs as a non-root user.
- Exposes port `8080`.
- Accepts JWT settings through environment variables.
  Expected commands:

```text
docker build -t humanit-interview-api .
docker run -p 8080:8080 humanit-interview-api
```

## 19. Build and Run Plan

From the `api` directory:

```text
mvn clean test
mvn spring-boot:run
```

API available at:

```text
http://localhost:8080/api/v1
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

H2 console in local profile:

```text
http://localhost:8080/h2-console
```

## 20. Code Quality Plan

Use lightweight tooling that supports maintainability without slowing the exercise down.
Recommended Maven checks:

- Maven Enforcer for Java and Maven version rules.
- Spotless or Checkstyle for consistent formatting.
- Standard Maven test lifecycle for automated verification.
  Avoid:
- Lombok, unless there is a strong reason to reduce boilerplate.
- Complex domain abstractions not required by the exercise.
- A separate user-management subsystem.
- Exposing JPA entities through controllers.

## 21. Implementation Order

Build the API incrementally so each step is testable.

### Step 1 - Convert Maven project to Spring Boot

- Replace the placeholder Maven setup with Spring Boot parent/dependencies.
- Set Java 21.
- Add Spring Web, Validation, Data JPA, H2, Flyway, Security, springdoc-openapi, and test dependencies.
- Replace placeholder application class with Spring Boot application entry point.

### Step 2 - Add configuration

- Add application configuration for local and test profiles.
- Configure H2.
- Configure Flyway.
- Configure JPA schema validation.
- Configure JWT properties.

### Step 3 - Create database migration

- Add initial Flyway migration for `clients` and `documents`.
- Include primary keys, foreign keys, unique constraints, and non-null constraints.

### Step 4 - Implement domain entities and repositories

- Create `Client` entity.
- Create `Document` entity.
- Configure one-to-many and many-to-one relationship.
- Create `ClientRepository`.
- Create `DocumentRepository`.

### Step 5 - Implement DTOs and mappers

- Create request and response DTOs.
- Add validation annotations.
- Create mapper components.
- Keep entity conversion out of controllers.

### Step 6 - Implement global exception handling

- Add custom exceptions for not found and conflict cases.
- Add global exception handler.
- Standardize validation and error responses.

### Step 7 - Implement Client service

- Add create, list, get, update, and delete operations.
- Add transaction boundaries.
- Add duplicate checks.
- Ensure Client and Documents persist atomically.

### Step 8 - Implement Document service

- Add nested Document CRUD operations.
- Enforce Client ownership.
- Add duplicate document number checks.
- Add transaction boundaries.

### Step 9 - Implement security and login

- Add Spring Security configuration.
- Add configured user authentication.
- Add JWT generation and validation.
- Add login endpoint.
- Secure Client and Document endpoints.

### Step 10 - Implement controllers

- Add `AuthController`.
- Add `ClientController`.
- Add `ClientDocumentController`.
- Return correct status codes and `Location` headers.
- Wire pagination for Client list.

### Step 11 - Add OpenAPI documentation

- Add OpenAPI configuration.
- Add JWT bearer scheme.
- Ensure DTOs and error responses are documented.

### Step 12 - Add tests

- Add integration tests for auth.
- Add happy-path tests for every Client endpoint.
- Add happy-path tests for every Document endpoint.
- Add validation, error, auth, and transactional tests.

### Step 13 - Add Docker and run instructions

- Confirm Dockerfile builds the Spring Boot jar.
- Add run instructions to `README.md`.
- Document environment variables.

### Step 14 - Add AI development evidence

- Update `AI_DEVELOPMENT.md`.
- Include AI tools used, prompts, accepted/rejected suggestions, and reflection.

### Step 15 - Final review

- Run full test suite.
- Run quality checks.
- Review API behavior against `project.txt`.
- Review code for SOLID, readability, and separation of concerns.
- Confirm technical review talking points are covered.

## 22. Review Talking Points

Be prepared to explain:

- Why Client is the aggregate root.
- Why Documents are nested under Client routes.
- How the one-to-many relationship is modeled.
- How `@Transactional` guarantees atomicity at the service layer.
- Why DTOs are used instead of exposing JPA entities.
- Why Flyway is used for schema management.
- How JWT protects the Client and Document APIs.
- How validation and global exception handling produce consistent errors.
- How integration tests prove the API behavior.
- Where the solution intentionally avoids unnecessary complexity.
