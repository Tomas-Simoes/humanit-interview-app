# AI Development

## AI Tools Used

Codex AI coding agent was the primary AI-assisted development tool used throughout the project.

I initially attempted to use my preferred local workflow, OpenCode → Ollama → Qwen, but the model's performance on my machine was too slow for efficient development.

## Models Used

The primary model used through Codex was GPT-5.5.

## AI Usage

In my day-to-day development workflow, I primarily use AI as an assistant for information gathering and code assistance.

For this particular project, although I had previous experience building APIs with Node.js frameworks, I was not as familiar with API development in Java. However, with the help of AI, I was able to quickly understand the Java-specific concepts and apply knowledge from my previous experience.

When it comes to writing code, I generally prefer to build the initial architecture and core implementation myself, with some AI assistance for boilerplate code. This allows the AI to understand my coding style, architectural decisions, and preferred project structure. Once that foundation is established, I use AI to extend the application while maintaining consistency with my existing implementation.

I followed this approach when developing the Clients and Documents modules. I first implemented the Clients module myself, defining its architecture and structure. Afterwards, I instructed the AI to implement the Documents module by following the same architectural patterns and conventions. Naturally, this did not eliminate the need for code reviews, but accelerated the development process.

Another important part of my AI-assisted workflow is code review and continuous improvement. I tend to be very detail-oriented when developing software, and I always aim to build systems that are well-structured, maintainable, and reliable. For this reason, I frequently ask AI to perform comprehensive code reviews and generate reports evaluating the current state of the application. These reviews help me identify potential issues, architectural weaknesses, areas for improvement, and mistakes I might have overlooked.

Rather than simply asking AI to fix the issues it identifies, I try to understand why something is considered a problem and how the proposed solution improves the codebase. This often involves multiple rounds of discussion, where I question the AI's suggestions, evaluate the reasoning behind them, and decide whether they are appropriate for the project.

With that in mind, the following section highlights some of the most important prompts, discussions, and decisions made during the development of this application.

## Important Prompts and Instructions

### Planning and Architecture

Since this project involved a relatively simple API with only two entities and limited business logic, designing its architecture was fairly straightforward.

Nevertheless, as the project specifically emphasized AI-assisted planning, I decided to use AI during this stage. The prompt is available at `AI/prompts/planning.md`.

The result was useful in establishing a general understanding of the project's requirements and how to structure the application from the early stages. In particular, I took inspiration from the suggested package organization, potential API endpoints, and overall architectural approach.

### Client Layer Review

After implementing the initial Client layer, I asked AI to review its design before applying the same architectural patterns to the Documents module. The prompt used for this review is available at `AI/prompts/clientLayerReview.md`.

The main objective of this review was to evaluate whether my architectural decisions were appropriate, identify potential weaknesses, and verify that the implementation followed established software engineering principles and common best practices.

When conducting this type of review, I follow an iterative approach. I read the AI's feedback, try to understand the reasoning behind each criticism, and evaluate whether the identified issue is genuinely relevant to the application.

This process sometimes involves multiple rounds of discussion, where I challenge the AI's suggestions, ask for further explanations, and explore alternative solutions. My goal is to reach an implementation that I understand, consider well-designed, and feel confident maintaining.

### Testing

In my previous software development experience, I had not written many automated tests, mostly because the projects were not being built with a production-ready mindset. Because of that, I had to research testing strategies and use AI to understand what should be tested, where it should be tested, and how to avoid writing tests that were either too shallow or unnecessarily duplicated.

For this project, I considered higher-fidelity integration tests more valuable, as they allowed me to validate the interaction between real application components.

After implementing the initial integration tests for the repository, service, and API layers, I used AI assistance to review the overall test coverage. This review helped identify additional scenarios and edge cases that had not been covered by the original test suite.

I then asked the AI to help implement the remaining relevant tests suggested during the review. Once they were generated, I personally examined the implementations, evaluated their purpose, and removed or adjusted tests that I considered redundant or unnecessary.

### Final Application Review

Towards the end of development, I also used AI to perform several comprehensive reviews of the entire application, available at `AI/prompts/finalReview.md`

I repeated this process around three or four times. After each review, I analyzed the reported issues, evaluated the suggested improvements, and implemented the changes I considered necessary. I would then request another review to check whether the previous issues had been resolved and identify any remaining weaknesses.

This iterative process helped me progressively improve the application, verify its compliance with the project requirements, and bring its architecture, error handling, and overall code quality closer to production-ready standards.

## AI-generated suggestion

One AI-generated suggestion that I decided not to follow came during the testing phase was that AI suggested writing several mock-based unit tests for the service layer using Mockito. For example, one of the suggested tests looked like this:

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

After reviewing this approach, I decided not to adopt it. Although mock-based unit tests can be valuable in certain scenarios, I felt that this particular test provided limited confidence in the actual behaviour of the application.

Most of the dependencies were mocked, including the repository and mapper, and the expected response was predefined within the test itself. As a result, the test did not verify whether the client was actually persisted, whether the mapper behaved correctly, or whether the database constraints were enforced.

One AI suggestion I accepted was the overall application architecture. The AI recommended separating responsibilities into different layers, with controllers handling HTTP requests, services containing business logic, repositories managing database access, and mappers converting between entities and DTOs.

This was already close to the structure I had in mind, so I agreed with the suggestion and followed this approach for both Clients and Documents.

One AI suggestion I modified was the exception handling architecture. Initially, the AI suggested creating individual exception handlers for almost every possible error. Personally, I felt this was adding unnecessary complexity and boilerplate without much benefit.

Instead, I decided to centralize application-specific errors through a single `ApplicationException` handler, using an `ErrorCode` enum and a switch-based approach to determine the appropriate response. I kept dedicated handlers only for cases that genuinely required special treatment, such as validation errors, malformed requests, or framework exceptions.

## Strengths and Limitations of AI

The main strength of using AI during this exercise was its ability to accelerate development and learning. It helped me adapt my previous API development experience to Java, understand unfamiliar concepts, explore different strategies, and identify potential improvements through iterative code reviews.

However, its suggestions were not always appropriate for the project's scope, sometimes introducing unnecessary complexity or solutions that did not provide meaningful benefits. 

Overall, I found AI most valuable when treated as a development assistant while questioning its recommendations, understanding the reasoning behind them, and making the final technical decisions myself.
