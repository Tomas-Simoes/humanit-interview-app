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

# Controllers and Services

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

