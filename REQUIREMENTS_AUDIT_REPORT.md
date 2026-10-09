# Requirements Audit Report

Date: 2026-10-09  
Reviewer role: Senior Java backend developer  
Scope: `project.txt`, Java/Spring source code, tests, migrations, configuration, README, Dockerfile, Makefile, and AI evidence files in the current working tree.

## Verification

- Reviewed the backend application under `api/`, including controllers, services, repositories, DTOs, entities, security, error handling, configuration, migrations, and tests.
- Ran `make verify`.
- The first sandboxed run failed because `@SpringBootTest(webEnvironment = RANDOM_PORT)` could not start embedded Tomcat: `java.net.SocketException: Operation not permitted`.
- Reran `make verify` outside the sandbox because the failure was caused by the sandbox blocking random-port server binding.
- Final result after follow-up changes: build success, 117 tests run, 0 failures, 0 errors, 0 skipped.
- The previously untracked source/configuration files have been prepared for commit so the final submission can include the full working tree.

## Executive Summary

The current application satisfies the core requirements in `project.txt`. It is a Java 21 + Spring Boot REST API using Maven, H2, Flyway migrations, JPA, DTOs, validation, Client CRUD, nested Document CRUD, transactional service methods, JWT authentication, OpenAPI documentation, Docker support, and a meaningful automated test suite.

No major functional requirement is missing in the current working tree.

The main remaining production-readiness considerations are operational hardening items rather than mandatory exercise gaps.

For production readiness, the biggest improvement areas are configuration profiles, security hardening, concurrency handling, deeper observability, and production database strategy.

## Requirement Coverage

| Requirement | Status | Evidence | Notes |
| --- | --- | --- | --- |
| Java + Spring REST API | Done | `api/pom.xml`, `api/src/main/java/humanit/App.java` | Spring Boot 3.5.16 with Java 21. |
| Maven or Gradle build | Done | `api/pom.xml`, `Makefile` | `make verify` passes outside the restricted sandbox. |
| Client CRUD | Done | `ClientController` | `POST`, list `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` under `/api/v1/clients`. |
| Document management | Done | `DocumentController` | Provides nested document CRUD under `/api/v1/clients/{clientId}/documents`. |
| REST conventions | Done | Controllers and API tests | Uses `201 Created` with `Location`, `200 OK`, `204 No Content`, and structured `400/401/404/409` errors. |
| Client entity fields | Done | `Client` | Includes id, first name, last name, tax identifier, email, phone number, and documents. |
| Document entity fields | Done | `Document` | Includes id, number, description, expiration date, and owning client. |
| Client-Document one-to-many | Done | `Client`, `Document`, `V1__create_clients_and_documents.sql` | JPA relationship and DB foreign key with cascade delete are present. |
| Persist clients and documents atomically | Done | `@Transactional` service methods | Client creation with nested documents happens in one transaction; service tests verify rollback on duplicate nested document numbers. |
| H2 database | Done | `application.properties` | Uses `jdbc:h2:mem:humanit`. |
| Schema management | Done | Flyway migrations V1 and V2 | Hibernate uses `ddl-auto=validate`, which is reviewable and safer than implicit schema generation. |
| Automated happy-path API tests | Done | `ClientApiIT`, `DocumentApiIT`, `AuthApiIT` | Tests exceed the minimum and include validation, malformed input, conflicts, ownership checks, auth, pagination, and request id behavior. |
| Login API if JWT is implemented | Done | `AuthController`, `AuthService` | Provides `/api/v1/auth/register` and `/api/v1/auth/login`. |
| Secure Client APIs with JWT | Done | `SecurityConfig` | `/api/v1/clients/**` requires authentication; nested document routes are covered by that rule. |
| DTOs and mapping | Done | DTO packages and mapper classes | Persistence entities are not exposed directly. |
| Input validation | Done, improvable | Request DTOs | Covers required fields, sizes, email, and positive ids; production should add richer domain validation and normalization. |
| Global exception handling | Done | `GlobalExceptionHandler`, `ApiProblemFactory` | Uses `ProblemDetail` and domain error codes. |
| Structured error responses | Done, improvable | Error layer and security handlers | Application, validation, malformed request, auth, method, and media type errors are structured. Production should add request id and stronger constraint-specific conflict mapping. |
| Pagination | Done | `ClientPageResponse`, `PageResponse`, `PageableConfig`, `PageableGuard` | Stable response DTOs, max page size 100, and sort allowlists are present. |
| Swagger/OpenAPI optional enhancement | Done | `springdoc-openapi`, `OpenApiConfig`, annotations | Swagger/OpenAPI exists; production should gate or protect it. |
| Docker optional enhancement | Done, improvable | `Dockerfile`, `Makefile`, README | Multi-stage build, non-root runtime user, stdout-only container logging, and healthcheck are present; Docker build skips tests. |
| Build/run instructions | Done | `README.md`, `.env.example` | Covers local run, build, test, Docker, and JWT secret setup. |
| AI development evidence | Done | `AI/AI_DEVELOPMENT.md`, prompt files | Canonical evidence includes tools, model, prompts, accepted/modified/rejected suggestions, and reflection. |

## Missing Or Partial Against Requirements

No mandatory requirement remains missing in the reviewed working tree.

Optional static analysis is partially implemented: Spotless and JaCoCo run in Maven, and CI runs format, verification/coverage, and dependency vulnerability checks. A deeper static analyzer such as SpotBugs or PMD remains optional.

## Done But Should Be Improved For Production

1. Configuration and profiles

   Split local, test, and production configuration. H2, Swagger, devtools, and file logging are fine for the exercise but should not all be default production behavior. Add profile-specific properties, document required env vars, and fail fast on missing production configuration.

2. Security hardening

   JWT support is solid for the exercise: stateless security, issuer/audience validation, short-lived tokens, Base64 secret validation, and custom 401/403 problem responses. For production, avoid open public registration by default, add rate limiting, account lockout or throttling, stronger password policy, CORS configuration, refresh/revocation strategy if needed, and real role-based authorization.

3. Input normalization and domain validation

   Auth emails are normalized, but Client and Document inputs are mostly persisted as provided. Trim strings, normalize client email case, add tax identifier and phone validation, normalize document numbers, and decide whether expiration dates must be present or future. Without this, duplicate checks can be case-sensitive and data quality can drift.

4. Data integrity under concurrency

   Service-level uniqueness checks produce good messages, and DB constraints protect the data. Race-condition conflicts still fall back to a generic data-integrity error. Map constraint names to domain-specific `409` responses and consider `@Version` optimistic locking for concurrent updates.

5. Error response completeness

   `ProblemDetail` is a good foundation. Add the request id to error bodies, set `instance` where useful, handle missing routes consistently, and keep unexpected errors generic while logging enough internal context for diagnosis.

6. Observability

   Request IDs, request logging, and Actuator health are already present. Add metrics, structured JSON logs, tracing hooks, and production-safe logging levels.

7. OpenAPI production policy

   Swagger is useful for review and development. In production, expose it intentionally only in allowed environments or protect it. Also document auth, pagination, and error schemas thoroughly enough for generated clients.

8. Docker and runtime hardening

   The Dockerfile is a good start: multi-stage build, Java 21 runtime, non-root user, stdout-only container logging, and healthcheck. Add JVM/container memory options, explicit env-var documentation, vulnerability scanning, and avoid publishing images built with `-DskipTests` unless CI has already verified the same source.

9. CI and quality gates

   CI now runs formatting, verification/coverage, and dependency/security scanning. The build currently emits Mockito dynamic-agent warnings, so configure Mockito as a Java agent or remove the inline mock maker if it is unnecessary.

10. Production database strategy

   H2 is acceptable for the exercise. For production, add a real database profile, connection pool tuning, migration checks in deployment, and Testcontainers-based integration tests against the production database engine.

## Recommended Priority

1. Add production profiles and lock down Swagger, registration, CORS, and rate limiting.
2. Map DB constraint conflicts to domain-specific errors under race conditions.
3. Add deeper static analysis such as SpotBugs or PMD if desired.
4. Harden the Docker runtime further with JVM options and a CI-verified image build path.
