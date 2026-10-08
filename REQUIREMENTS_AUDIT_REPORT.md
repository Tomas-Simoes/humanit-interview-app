# Requirements Audit Report

Date: 2026-10-08
Role: Senior Java backend review

## Verification

- Reviewed `project.txt`, API source, migrations, configuration, Dockerfile, README, AI evidence, and tests.
- Ran `mvn clean verify` from `api/`.
- Result: build success, 93 tests run, 0 failures, 0 errors.
- Note: the clean verification needed to run outside the restricted sandbox because `@SpringBootTest(webEnvironment = RANDOM_PORT)` starts an embedded Tomcat server on a local random port.
- Note: the current working tree has uncommitted changes in `ClientApiIT` and `DocumentApiIT` that remove assertions for validation/malformed JSON problem response bodies. The passing result reflects the current working tree.

## Executive Summary

The application is a functional Java 21 + Spring Boot REST API. It implements Client CRUD, nested Document CRUD, H2 persistence, Flyway migrations, DTOs, validation annotations, JWT login/register, secured client APIs, and a substantial automated test suite.

The largest remaining requirement gaps are deliverable/documentation gaps. Client creation now supports initializing Documents in the same request and persists the aggregate in a single transaction.

## Requirement Coverage

| Requirement | Status | Evidence | Notes |
| --- | --- | --- | --- |
| Java + Spring REST API | Done | `api/pom.xml`, `api/src/main/java/humanit/App.java` | Spring Boot 3.5.16, Java 21. |
| Maven build tool | Done | `api/pom.xml`, `Makefile` | `mvn clean verify` passes. |
| Client CRUD | Done | `ClientController` exposes POST, GET list, GET by id, PUT, DELETE under `/api/v1/clients`. | URI differs from `/clients`, but the requirements allow a clear consistent design. |
| REST status codes and payloads | Done | Create returns 201 with `Location`, update/get return 200, delete returns 204. | Uses DTO responses rather than exposing entities. |
| Client fields | Done | `Client` has id, firstName, lastName, taxIdentifier, email, phoneNumber, documents. | Unique constraints exist for email and tax identifier. |
| Document fields | Done | `Document` has id, number, description, expirationDate, client. | Nested document endpoints are implemented. |
| One-to-many Client to Documents | Done | `Client.documents` has `@OneToMany`, `Document.client` has `@ManyToOne`. | JPA cascade and DB cascade delete are present. |
| H2 database | Done | `application.properties` configures `jdbc:h2:mem:humanit`. | H2 console is enabled in the default config. |
| Schema management | Done | Flyway migrations `V1__create_clients_and_documents.sql`, `V2__create_app_users.sql`; `ddl-auto=validate`. | Good reviewable approach. |
| Atomic persistence of Clients and Documents | Done | `CreateClientRequest` accepts `documents`; `ClientMapper` attaches them with `client.addDocument(...)`; `ClientService.createClient` saves the aggregate in one transaction. | Duplicate document numbers in the create payload are rejected before persistence. |
| Automated endpoint tests | Done | `ClientApiIT`, `DocumentApiIT`, `AuthApiIT`; verified by Maven. | Coverage is above the minimum happy path, but validation/malformed JSON response-body assertions are currently not active. |
| Login API if JWT is implemented | Done | `/api/v1/auth/login` and `/api/v1/auth/register`. | Login returns bearer JWT. |
| Secure Client APIs with JWT | Done | `SecurityConfig` protects `/api/v1/clients/**`. | Document endpoints are covered because they are nested under clients. |
| Dockerfile if Docker support is implemented | Partial | `Dockerfile` exists. | README lacks Docker instructions; Docker build/run was not verified during this audit. |
| API documentation with Swagger/OpenAPI | Missing optional enhancement | No `springdoc-openapi` dependency or Swagger config found. | Optional in `project.txt`, but useful for review. |
| AI development evidence | Partial | `AI/AI_DEVELOPMENT.md`, `AI/AI_DEVELOPMENT_2.md`, prompts under `AI/prompts/`. | Requirement asks for a short `AI_DEVELOPMENT.md`; currently there is no root-level file. |
| Build/run instructions | Missing | `README.md` only contains the project title. | This is a deliverable gap. |

## Missing From The Requirements

1. Complete README

   `project.txt` requires instructions for building and running the project. `README.md` currently only has the title. Add:

   - Project purpose and stack.
   - Java/Maven prerequisites.
   - Local run command.
   - Test command.
   - Build command.
   - Auth flow with register/login.
   - Example requests for Client and Document endpoints.
   - Error response format.
   - H2 console details.
   - Docker build/run instructions if Docker remains part of the submission.
   - Link to AI development evidence.

2. Root AI development evidence file

   The evidence exists under `AI/`, and `AI/AI_DEVELOPMENT_2.md` looks closer to the requested final form. The requirement asks for a short `AI_DEVELOPMENT.md`; place the polished version at the repository root or explicitly document why it lives under `AI/`.

3. Swagger/OpenAPI

   This is optional, not a hard blocker. It is still missing and would improve reviewer usability.

4. Docker instructions and verification

   A Dockerfile exists, but the README does not explain it. The Dockerfile also skips tests during image build, so CI should run Maven verification before image creation.

## Done But Should Be Improved For Production

1. Configuration and secrets

   `application.properties` contains a hardcoded JWT secret and enables SQL logging plus the H2 console. Move secrets to environment variables or a secret manager, add profiles such as `local`, `test`, and `prod`, disable the H2 console outside local development, and turn off SQL logging by default.

2. Error handling consistency

   `GlobalExceptionHandler` handles `ApplicationException` and `DataIntegrityViolationException`, but production APIs should explicitly handle validation errors, malformed JSON, authentication failures, authorization failures, unsupported methods, unsupported media types, and unexpected exceptions with one consistent problem schema and request correlation id.

3. Validation and normalization

   Current DTO validation covers blank, email, size, and required fields. Add business validation for tax identifier format, phone number format, case-insensitive/normalized email uniqueness, trimmed inputs, and document expiration date rules such as `@FutureOrPresent`.

4. Pagination contract

   Pagination is implemented, but controllers return Spring `Page` directly and have TODOs for page-size caps and pagination DTOs. Add a stable response DTO, maximum page size, default sorting rules, and validation for invalid paging/sorting parameters.

5. Security hardening

   JWT auth is implemented, but production should add rate limiting for login/register, stronger password policy, account lockout or throttling, refresh-token strategy if sessions need to last, CORS policy, and a controlled user provisioning story. Public registration may be fine for an exercise but is usually not a production default.

6. Data integrity under concurrency

   The service checks uniqueness before saving, and the database has unique constraints. Keep the DB constraints as the source of truth and improve conflict mapping by inspecting constraint names. Consider optimistic locking (`@Version`) if concurrent updates matter.

7. Observability

   Add structured application logs, request ids, health/readiness endpoints through Actuator, metrics, and production-safe log levels. The current default SQL/debug output is too noisy for production.

8. Docker and deployment

   The Dockerfile is a good start: multi-stage build, Java 21 runtime, non-root user. Improve it with Docker usage docs, a healthcheck, runtime environment configuration for secrets, and CI that verifies tests before building the image.

9. Static analysis and coverage

   Add Checkstyle or Spotless, SpotBugs or PMD, and JaCoCo coverage reporting. The tests are strong for an interview exercise, but production readiness benefits from automated quality gates.

10. API documentation and examples

   Add `springdoc-openapi` and expose Swagger UI in local/dev. Document authentication, pagination, error responses, and example payloads.

## Recommended Priority

1. Fill in `README.md`.
2. Promote the polished AI evidence to root `AI_DEVELOPMENT.md`.
3. Add OpenAPI documentation.
4. Externalize secrets and split local/test/prod configuration.
5. Add explicit production-grade error handling and pagination DTOs.
