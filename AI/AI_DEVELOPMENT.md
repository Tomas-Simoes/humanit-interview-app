AI Tools used: Codex 5.5 (tried using OpenCode Qwen however it was too slow on my machine and couldn't make a proper free Agent to work, so prefered to go with free Codex version)

Started by setting up the project asking the AI on which technologies I needed to install and how to:

"_project especifications attached_ Given this project struture and especifications, what do I need to setup/install for it? Give me concrete linux fedora commands."

Since Java is not my main programming language, asked about the conventions of API building and structuring

"What are the Java and REST convenctions for API building and structuring? Include folder structure and code conventions"

After analyzing it, started planning and setting up the project:

# First Planning

The input is available at AI/prompts/planning.md

After analyzing the requirements and setting up the project structure, I started by implementing the database layer. I chose to begin there because the Client and Document models define the core domain of the API, and having the persistence layer ready early makes it easier to build and test the API endpoints.

I initially considered a workflow similar to Prisma, where the data model is defined first and migrations are generated from it. With AI assistance, I researched the common approaches in Java/Spring and decided to use JPA entities together with Flyway migrations and Hibernate schema validation.

The final approach was:

- JPA entities define the Java domain model and relationships.
- Flyway migrations define and version the database schema explicitly.
- Hibernate runs with `ddl-auto=validate` to ensure the Java mappings match the database schema.

I created the initial Client and Document entities, the first Flyway migration, and the corresponding Spring Data JPA repositories. AI assistance was useful for checking JPA relationship syntax, migration structure, and common best practices around bidirectional relationships.

I also asked about reducing Java boilerplate for getters, setters, and constructors. Based on that, I added Lombok to the project.

# Client & Document Controllers and Services

Since Spring Boot is not the framework I am most experienced with, I used AI throughout the implementation mainly as a learning and support tool. I used it to understand Spring Boot conventions, API design patterns, annotations, syntax, and common best practices. The implementation was built my me.

The Client layer was divided into the following components:

Client entity
Client DTOs
ClientController
ClientService
ClientRepository
ClientMapper

The implementation itself was relatively straightforward. Whenever I encountered something specific to Spring Boot or JPA that I was unfamiliar with, I used AI to understand the concept and then applied it to the project.

Before moving on to the Document layer, I decided to perform a dedicated review of the Client implementation. The goal was to verify that the architecture I had chosen was appropriate and to identify potential problems or better practices before repeating the same patterns in the Document layer.

For larger tasks like code reviews, I prefer to first use an AI agent to create a detailed prompt. I find this useful because it helps define clear review criteria and reduces the chance of overlooking important aspects of the implementation.

I gave the agent the following input:

"I'm making a REST API to manage Client and Client Documents.
I already created the Client layer with DTOs to create a client, a ClientController, ClientService, ClientMapper and ClientRepository.

Before moving on to Documents, I want you to build me an optimized prompt to check this layer and identify potential problems and other best practices I could use.

I essentially want to know whether this is a good way of building this type of software and whether anything should be changed.

Known problems:

Client DTOs still do not contain the Document property. I will add it after creating the Document layer.
I am still using IllegalArgumentException. I will later replace these with explicit/domain-specific exceptions.

Project context: [project requirements]"

The generated prompt instructed another agent to review the implementation from several perspectives.

The output is available at AI/prompts/clientLayerReview.md

During the code review process, I follow an iterative approach for each issue identified by the AI. Rather than immediately accepting the suggested change, I first try to understand what the problem is, why it is considered a problem, and what impact it could have on the application.

This often involves several rounds of discussion. The AI identifies an issue, I ask for the reasoning behind it, and then continue asking follow-up questions until I fully understand the underlying concept and the recommended solution.

I find this approach more effective than simply asking the AI to fix the code automatically. The goal is not only to correct the current implementation, but also to understand the reasoning behind each improvement so that I can avoid repeating the same mistakes in future parts of the project.

After fixing everything, I just coppied the same pattern for the Documents layer, since it was basically the same features between Documents and Clients, I ask an AI to copy the exact pattern from clients to documents:

"Check the existing Client layer and use it as the structural template to build a new Document layer.

The Document layer should mirror the Client layer as closely as possible, including the same architecture, file organization, patterns, features, validation style, API structure, services, repositories, DTOs/types, hooks, UI patterns, tests, and error handling where applicable.

Requirements:
- First, analyze how the Client layer is implemented.
- Identify every feature and responsibility the Client layer has.
- Create the equivalent Document layer using the same conventions.
- Replace Client-specific naming, fields, routes, and logic with Document-specific equivalents.
- Keep the implementation consistent with the existing codebase.
- Do not introduce unrelated refactors or new architectural patterns.
- Ensure imports, exports, routes, and registrations are fully wired.

Before editing, briefly summarize the Client layer structure you found and your implementation plan. After editing, provide a concise summary of changed files and verification steps."

# Errors

The next phase was building the error layer. I wanted a clean and uncomplicated way to handle errors consistently across the whole application, without spreading `try/catch` blocks.

I asked AI about common Spring Boot error-handling patterns and we compared a few options

The approach that made the most sense was to use a global handler with `@RestControllerAdvice`. The service layer can throw application errors, and the error layer is responsible for translating those errors into proper HTTP responses.

The final result is an error layer where:

- services describe what went wrong by throwing `ApplicationException`
- `ErrorCode` identifies the type of application error
- `GlobalExceptionHandler` converts those errors into consistent HTTP responses
- controllers stay focused on request and response flow

# Testing

In my previous software development experience, I had not written many automated tests, mostly because the projects were not being built with a production-ready mindset. Because of that, I had to research testing strategies and use AI to understand what should be tested, where it should be tested, and how to avoid writing tests that were either too shallow or unnecessarily duplicated.

At first, the test suite started to feel confusing. Some behaviours appeared to overlap between different test classes. For example, duplicate client data could be tested at the repository level, the service level, and the API level and it felt redundant for me, but after discussing the architecture with AI I arrived at a concent

The final testing strategy was organized by application layer:

- `ClientRepositoryIT` focuses only on persistence behaviour. It verifies database and JPA concerns such as unique constraints, entity relationships, and custom queries like the client summary `documentCount`.
- `ClientServiceIT` focuses on business behaviour. It tests the real service, mapper, repository, and database working together, without going through HTTP. 
- `ClientApiIT` focuses on the external API contract. It verifies that real HTTP requests return the correct status codes,response bodies etc...

One AI-generated suggestion I rejected happened during the testing phase. Initially, the AI suggested writing several mock-based unit tests for the service layer using Mockito. For example, the suggested test looked like this:
@ExtendWith(MockitoExtension.class)
class ClientServiceTest {
    @Mock
    ClientRepository clientRepository;
    @Mock
    ClientMapper clientMapper;
    @InjectMocks
    ClientService clientService;

    @Test
    void createClient_savesClientWhenEmailAndTaxIdentifierAreUnique() {
        var request = new CreateClientRequest(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000");

        var client = new Client(
                "Ana", "Silva", "TAX-1", "ana@example.com", "910000000");

        var response = new ClientResponse(
                1L, "Ana", "Silva", "TAX-1", "ana@example.com", "910000000");

        when(clientRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(clientRepository.existsByTaxIdentifier("TAX-1")).thenReturn(false);
        when(clientMapper.toEntity(request)).thenReturn(client);
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toResponse(client)).thenReturn(response);

        ClientResponse result = clientService.createClient(request);

        assertThat(result).isEqualTo(response);
        verify(clientRepository).save(client);
    }
}

After reviewing this approach, I decided not to use it as the main testing strategy. Although mock-based unit tests can be useful in some cases, this specific test felt weak for this project. Most of the behaviour was being defined inside the test itself. The repository was mocked, the mapper was mocked, and the returned response was also mocked. Because of that, the test was not proving that the client was actually persisted, that the mapper worked correctly, or that the database constraints were valid. It mainly proved that Mockito returned the values that were configured inside the test.

Instead, I chose to use higher-fidelity integration tests.

After integrating the basic tests for the service, repository, and API layers, I reviewed the test coverage with AI assistance. That review identified several additional scenarios that were not covered by the initial test suite.

I then asked the AI to help integrate the remaining tests suggested by the review. After that, I personally reviewed the generated tests and removed or adjusted the ones that felt redundant. 