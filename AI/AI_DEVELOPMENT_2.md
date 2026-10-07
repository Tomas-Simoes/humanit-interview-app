# AI Development Evidence

This document summarizes how AI was used during the development of the Java + Spring REST API. 

## AI Tools Used

- Codex AI coding agent was the main AI-assisted development tool.
- I attempted to use the preferred local workflow, OpenCode -> Ollama -> Qwen, but it was too slow and unreliable on my machine for this exercise.
- Because the project required an agent that could inspect files, suggest changes, run builds/tests, and iterate on failures, I used Codex for the main agentic workflow.

## Models Used

- Codex 5.5 was used for the main implementation support, review, and debugging workflow.
- Qwen through Ollama was evaluated locally, but I did not use it as the main implementation agent because the local setup was not practical enough for the time available.

## Important Prompts and Instructions

I used AI iteratively instead of asking it to generate the whole project in one step.

### Environment and Setup

Early in the project, I asked AI what I needed to install and configure:

```text
[project specifications attached]
Given this project structure and specifications, what do I need to setup/install for it?
Give me concrete Linux Fedora commands.
```

Since Java and Spring Boot are not my strongest stack, I also asked for guidance on common Java REST API conventions:

```text
What are the Java and REST conventions for API building and structuring?
Include folder structure and code conventions.
```

### Planning and Architecture

Before writing code, I asked the agent to analyze `project.txt` and produce an implementation plan and architecture. The prompt used for this phase is stored in:

```text
AI/prompts/planning.md
```

The goal was to clarify the layered architecture, package structure, endpoints, DTOs, persistence model, transactions, testing strategy, and implementation order before starting the code.

### Client Layer Review

After implementing the initial Client layer, I asked AI to review the design before repeating the same pattern for Documents. The prompt used for that review is stored in:

```text
AI/prompts/clientLayerReview.md
```

The review focused on:

- Controller, service, repository, mapper, DTO, and entity responsibilities.
- REST conventions and HTTP status codes.
- Transaction boundaries.
- Validation and error handling.
- Whether the design was appropriate for a small but production-style Spring Boot API.

### Document Layer Guidance

For the Document layer, I asked AI to use the Client layer as a structural reference while keeping the implementation scoped to backend concerns:

```text
Check the existing Client layer and use it as the structural template to build a new Document layer.

The Document layer should mirror the Client layer where appropriate, including architecture,
file organization, validation style, API structure, services, repositories, DTOs, mapping,
tests, and error handling.

First analyze how the Client layer is implemented. Then create the equivalent Document layer
using Document-specific fields, routes, ownership rules, and repository logic.
Keep the implementation consistent with the existing codebase and do not introduce unrelated
refactors or new architectural patterns.
```

## How AI Contributed

AI contributed mainly as a development assistant and reviewer, not as a replacement for understanding the code.

- Planning: helped break the requirements into an implementation plan and identify the main architecture.
- Implementation: helped clarify Spring Boot, JPA, validation, Flyway, DTOs, mappers, and REST conventions.
- Testing: helped compare repository, service, mapper, and API-level tests and decide what each layer should prove.
- Debugging: helped interpret compilation and test failures during iterative development.
- Review: helped identify possible design problems before extending the Client pattern to the Document layer.

## Accepted, Modified, and Rejected AI Suggestions

One suggestion I accepted was to use a layered structure with controllers, services, repositories, DTOs, and mappers. This matched the project requirements and kept HTTP handling, business logic, persistence, and mapping responsibilities separated.

One suggestion I modified was the database schema approach. I initially thought about a workflow similar to Prisma, where the model drives generated migrations. After using AI to compare common Spring approaches, I chose explicit Flyway migrations with Hibernate `ddl-auto=validate`. This kept the schema versioned and reviewable while still allowing Hibernate to verify that the JPA mappings match the database.

One suggestion I rejected was making mock-based Mockito unit tests the main service testing strategy. The AI proposed tests where the repository, mapper, and returned response were all mocked. I decided that this was too weak for this exercise because it mostly tested Mockito setup rather than real persistence, mapping, validation, or transaction behavior. Instead, I focused more on higher-fidelity tests, such as repository, service integration, mapper, and API tests.

## Reflection

Using AI was most useful for navigating unfamiliar Spring Boot conventions, validating architectural decisions, and getting review feedback while building the project incrementally. It helped me move faster, especially when deciding how to structure DTOs, services, repositories, error handling, and tests.

The main limitation was that AI suggestions still needed careful review. Some suggestions were too generic, too complex for the exercise, or not aligned with the current implementation stage. I had to keep checking the project requirements, run the code/tests, and decide which suggestions were actually appropriate.

Overall, AI was valuable as a planning, implementation, debugging, and review assistant, but I remained responsible for understanding the code, validating the design, and deciding what to accept, modify, or reject.
