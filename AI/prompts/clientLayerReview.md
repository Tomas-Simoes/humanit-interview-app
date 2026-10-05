You are a Senior Java / Spring Boot Backend Engineer reviewing an existing REST API implementation.
I am building a REST API to manage Clients and their Documents using Java + Spring.
I have already implemented the Client layer, including:
- Client entity
- Client DTOs
- ClientController
- ClientService
- ClientMapper
- ClientRepository
Before implementing the Document layer, review the current Client implementation and determine whether the architecture and implementation are appropriate for a clean, maintainable, production-style Spring REST API.
Context
The API must support:
- POST /clients
- GET /clients
- GET /clients/{id}
- PUT /clients/{id}
- DELETE /clients/{id}
A Client contains:
- Id
- FirstName
- LastName
- TaxIdentifier
- Email
- PhoneNumber
- Documents — one-to-many relationship
A Document will contain:
- Id
- Number
- Description
- ExpirationDate
A Client may have multiple Documents.
The project will eventually also include:
- H2 database
- Flyway migrations
- Input validation
- Global exception handling
- Structured error responses
- Swagger / OpenAPI
- JWT authentication
- Automated tests
- Docker
The main priorities are:
- SOLID principles
- Separation of concerns
- REST conventions
- Clean architecture
- Maintainability
- Readability
- Testability
- Avoiding unnecessary complexity or over-engineering
Known incomplete areas
Do not report these as unexpected problems:
1. Client DTOs currently do not contain Documents. I will add them after implementing the Document layer.
2. Some service errors currently use IllegalArgumentException. This is temporary and will later be replaced with domain-specific exceptions and global exception handling.
3. The only operations available are createClient and getClient
Review the Client layer
Inspect all Client-related files and evaluate:
1. Architecture
Check whether the current separation between:
Controller -> Service -> Repository
with DTOs and a Mapper is appropriate.
Identify:
- unnecessary layers
- missing responsibilities
- responsibilities placed in the wrong layer
- excessive coupling
- violations of separation of concerns
- SOLID violations
2. Controller
Check:
- endpoint design
- HTTP methods
- status codes
- request/response handling
- use of DTOs
- validation placement
- whether business logic has leaked into the controller
3. Service
Check:
- business logic placement
- transaction boundaries
- repository usage
- entity update logic
- duplicate checks
- error handling
- unnecessary database queries
- race-condition risks where relevant
Do not recommend complexity that is unnecessary for this exercise.
4. Repository
Check:
- Spring Data JPA usage
- unnecessary custom queries
- naming conventions
- duplicate/existence queries
- whether database constraints should handle anything currently handled only in Java
5. DTOs
Check:
- separation between request and response DTOs
- which fields clients should be allowed to submit
- validation annotations
- whether IDs or internal persistence details are exposed unnecessarily
- DTO naming and structure
6. Mapper
Check:
- mapping responsibilities
- create vs update mapping
- whether mapping logic is duplicated
- whether the mapper should remain manual or use another approach
Do not recommend MapStruct unless it provides a meaningful benefit for a project of this size.
7. Entity / JPA
Check:
- entity structure
- column constraints
- ID generation
- unique constraints
- nullability
- JPA annotations
- equality/hashCode concerns
- future Client -> Documents relationship design
- cascade/orphan removal implications
8. REST API design
Check whether the Client API follows common REST conventions and whether anything should change before implementing Documents.
Consider the future Document endpoints, for example:
- /clients/{clientId}/documents
- /clients/{clientId}/documents/{documentId}
but do not implement the Document layer yet.
9. Maintainability and best practices
Identify improvements that would make the implementation more:
- readable
- testable
- maintainable
- consistent
- idiomatic for modern Spring Boot
Avoid suggestions that exist only for enterprise-scale systems and provide little value in this exercise.
Output format
Start with an overall assessment:
Overall: Excellent / Good / Acceptable / Needs Improvement
Then organize findings into:
Critical Problems
Problems that should be fixed before continuing.
Important Improvements
Changes that significantly improve correctness, architecture, maintainability, or REST design.
Minor Improvements
Naming, readability, style, or small best-practice improvements.
Good Decisions
Things that are already implemented correctly and should remain unchanged.
Before Implementing Documents
A short checklist of changes worth making now before building the Document layer.
For every identified issue:
1. Point to the relevant file/class/method.
2. Explain what is wrong.
3. Explain why it matters.
4. Recommend the simplest appropriate fix.
5. Only provide a small code example when it helps explain the change.
Do not rewrite the entire Client layer unless there is a strong architectural reason.
Do not change working code purely because another style is possible.
Prioritize practical engineering decisions over theoretical perfection.
Finally answer:
If you were reviewing this as a real junior backend developer project, would you approve this Client layer before allowing development of the Document layer? Why or why not?