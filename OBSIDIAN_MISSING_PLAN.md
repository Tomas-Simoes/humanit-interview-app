---
title: Humanit Interview API - Current State and Missing Work Plan
created: 2026-10-07
status: draft
tags: [humanit, spring-boot, api, project-plan, obsidian]
---

# Humanit Interview API - Current State and Missing Work Plan

## Summary
The project is already in a good state. It has a working Java 21 Spring Boot REST API, Maven build configuration, H2 persistence, Flyway migrations, DTOs, validation, service-level transactions, global error handling, nested Document endpoints, Docker support, and a passing full verification run.

This is not a project that needs to be rebuilt from zero. The remaining work is mainly about finishing the deliverable, closing one important functional gap around Client Documents, and deciding whether optional features such as JWT authentication and OpenAPI documentation should be included.

The most important missing piece is Client aggregate document support. The code already has a real Client-to-Document relationship and separate nested endpoints for Documents, but the Client create, update, and response DTOs do not yet include Documents. Because the requirements mention persisting Clients and their Documents atomically, this should be treated as the main functional gap.

## Current State
The API currently supports full Client CRUD under `/api/v1/clients`. It also supports full nested Document CRUD under `/api/v1/clients/{clientId}/documents`. The persistence layer is built with JPA entities, Spring Data repositories, an H2 in-memory database, and a Flyway migration that creates the `clients` and `documents` tables.

The Client entity has the required fields: `id`, `firstName`, `lastName`, `taxIdentifier`, `email`, `phoneNumber`, and `documents`. The Document entity has the required fields: `id`, `number`, `description`, `expirationDate`, and `client`. The one-to-many relationship exists in the domain model, and deleting a Client deletes its Documents through cascade and orphan removal.

The service layer owns transaction boundaries with `@Transactional`, which is the correct place for them. Create, update, and delete operations are transactional. Read operations are marked as read-only. This means the application already has the right foundation for atomic operations.

The API uses DTOs instead of exposing JPA entities directly. Validation exists on the current create and update requests. Errors are handled centrally through `GlobalExceptionHandler`, and the API returns Spring `ProblemDetail` responses for validation, not-found, conflict, malformed request, data-integrity, and unexpected server errors.

The test suite is also in a strong state for the features that currently exist. There are mapper tests, entity relationship tests, repository integration tests, service integration tests, API integration tests, startup verification, and error response tests.

## Verification Result
I ran the full verification command from the `api/` directory.

```bash
mvn clean verify
```

The build passed. Surefire ran 8 tests with 0 failures and 0 errors. Failsafe completed 86 integration-test executions with 0 failures and 0 errors.

One environment note: the API integration tests start embedded Tomcat on random local ports. Inside the restricted sandbox this fails with `java.net.SocketException: Operation not permitted`, but outside the sandbox the same clean verification passes successfully.

## What Is Missing
The README is the clearest deliverable gap. At the moment it only contains the project title, so it is not ready for submission. It should explain what the project does, how to install or prepare the environment, how to run the API, how to run tests, how to build the jar, how to use Docker, what endpoints exist, what the request and response bodies look like, where the H2 console is, how errors are returned, and where the AI development evidence can be found.

The second major gap is Client-with-Documents behavior. The database and entity model support Documents, and the nested Document endpoints work, but `POST /api/v1/clients` currently creates only Client fields. `PUT /api/v1/clients/{id}` updates only Client fields. `GET /api/v1/clients/{id}` returns only Client fields. To fully satisfy the aggregate requirement, Client create and read should include Documents, and Client update should either clearly remain Client-only or intentionally replace the full Client aggregate.

If the aggregate update approach from the implementation plan is kept, `CreateClientRequest` should accept nested `CreateDocumentRequest` values, `UpdateClientRequest` should accept document update values, and `ClientResponse` should include nested `DocumentResponse` values. Creating a Client with Documents should attach each Document through `client.addDocument(...)` and save the whole aggregate in one transaction. Updating the aggregate should update submitted existing Documents, create submitted new Documents, remove omitted existing Documents, and reject document IDs that belong to another Client.

Tests should then be added for this aggregate behavior. The important cases are creating a Client with Documents, fetching a Client with Documents, rejecting duplicate document numbers inside the same Client request, validating nested Document fields, rolling back the whole Client creation if a nested Document fails, replacing Documents during update, and rejecting document IDs owned by another Client.

Pagination works, but it is not fully polished. The controllers currently return Spring `Page` directly. The implementation plan mentions a custom `PageResponse<T>` DTO, page-size limits, and safe sort handling. This is not urgent because the current API works, but owning the response shape would make the API contract cleaner.

The AI development evidence also needs cleanup. There are two files: `AI/AI_DEVELOPMENT.md` and `AI/AI_DEVELOPMENT_2.md`. The second one is more polished. The requirement asks for a short `AI_DEVELOPMENT.md`, so the cleanest final state would be one polished root-level `AI_DEVELOPMENT.md`, linked from the README, with `AI/prompts/` kept as supporting evidence.

Docker support exists, but it still needs to be documented and verified. The Dockerfile is present, and the Makefile has `docker-build` and `docker-run` targets. The README should explain those commands, and the container should be smoke-tested before final submission.

The implementation plan and the code are not fully aligned. `IMPLEMENTATION_PLAN.md` describes JWT authentication, OpenAPI, a custom pagination response, and Client responses with Documents. The current code does not include those things yet. Before submission, either those features should be implemented or the plan should be edited so it accurately reflects the final scope.

JWT authentication is currently missing. This is optional in `project.txt`, but the local implementation plan treats it as part of the target architecture. There is no `auth` package, no login endpoint, no Spring Security dependency, and no JWT generation or validation. This should be a deliberate decision: either implement it properly with tests or explicitly leave it out as an optional enhancement.

OpenAPI/Swagger is also missing. This is optional, but useful for interview review. If added, it should come after the core aggregate work. If JWT is implemented, the OpenAPI configuration should also document the bearer token security scheme.

## Clean Plan
The first step should be finishing the README. This is the fastest way to make the project reviewable. The README should explain the API purpose, the stack, the local run command, the test command, the build command, the Docker commands, the endpoint structure, the error format, and the AI evidence location.

After that, clean up the AI evidence. Choose one final version, polish the wording, and make sure it explains the tools used, the models used, the important prompts, how AI helped, one accepted suggestion, one modified suggestion, one rejected suggestion, and a short reflection.

The next step should be Client aggregate document support. Add Documents to the Client request and response DTOs, then update the mapper and service logic so a Client can be created with Documents in one transaction. Decide whether `PUT /clients/{id}` should replace the whole aggregate or only update Client fields. If it replaces the aggregate, implement document create, update, and removal inside the same transaction.

Once that behavior exists, extend the tests. Service tests should prove persistence, duplicate handling, ownership checks, and rollback behavior. API tests should prove the HTTP contract, including status codes and response bodies.

After the core behavior is correct, polish pagination. Replace direct `Page<T>` responses with a small project-owned `PageResponse<T>` DTO, add a maximum page size, and validate sort fields. This makes the API contract cleaner and avoids exposing Spring's internal page response shape as the public contract.

Then decide the optional scope. If time is short, leave JWT and OpenAPI out and document them as optional enhancements not implemented. If time allows, implement JWT first because it changes how the API is used, then add OpenAPI afterward so the documentation matches the secured API.

The final step should be verification. Run `make verify`, `make build`, `make docker-build`, and `make docker-run`. After the container starts, smoke test the Client endpoint with `curl`. The final submission should have passing tests, a complete README, one final AI evidence document, verified Docker instructions, and either implemented optional features or a clear statement that they were intentionally left out of scope.

## Final Target
The final project should be easy for a reviewer to run, test, and understand. Client CRUD should work. Document CRUD should work. Client creation with Documents should be atomic. Error responses should be consistent and documented. AI usage should be clearly explained. Docker should be verified.

JWT and OpenAPI are optional. They should only be included if they are implemented cleanly and tested. A smaller project where the README, plan, and code agree is better than a larger project where the documentation promises features that are not actually implemented.

