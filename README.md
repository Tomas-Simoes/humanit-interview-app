# HumanIT Interview App

Spring Boot API for managing clients and their documents. The service uses Java
21, Spring Security with JWT authentication, Flyway migrations, and an in-memory H2 database.

## Components

- Auth API: register a user and log in to receive a JWT bearer token.
- Client API: create, list, retrieve, update, and delete clients.
- Document API: list all documents and manage documents nested under a client.
- Persistence: H2 in-memory database, Flyway migrations, and JPA entities.
- Health endpoint: `/actuator/health`.
- API docs: OpenAPI/Swagger UI is available at `/swagger-ui.html` while the app is running.

The normal flow is: register or log in, use the returned bearer token in the
`Authorization` header, then call the client and document endpoints.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker, optional, for container builds
- `make`, optional, for the shortcut commands below

## Configuration

The API needs `APP_SECURITY_JWT_SECRET` to sign and validate JWT tokens. The
value must be a Base64-encoded HMAC secret. For local development, copy the
example file:

```sh
cp .env.example .env
```

The Makefile loads `.env` automatically for local commands. 

## Run Locally

From the repository root:

```sh
make run
```

The API starts on `http://localhost:8080`.

It uses an in-memory H2 database, so data is reset when the application stops.

## Build

Create the application jar:

```sh
make build
```

The built jar is written to `api/target/`.

Run the packaged application:

```sh
java -jar api/target/api-0.0.1-SNAPSHOT.jar
```

## Test

Run the unit test suite:

```sh
make test
```

Run the full Maven verification lifecycle, including integration tests:

```sh
make verify
```

## Docker

Build the image:

```sh
make docker-build
```

Run the container:

```sh
make docker-run
```

The container exposes the API at `http://localhost:8080`. Docker uses the same
configuration as local development: `make docker-run` passes `.env` to the
container with `--env-file`, so `APP_SECURITY_JWT_SECRET` must be present there.

Implementation note: Multi-stage build, non-root runtime user, stdout-only
container logging, and a container healthcheck are present; Docker build skips tests.

## Make Targets

- `make run` - start the Spring Boot application
- `make compile` - compile the API
- `make test` - run unit tests
- `make verify` - run the Maven verification lifecycle
- `make build` - clean and package the jar
- `make clean` - remove Maven build output
- `make docker-build` - build the Docker image
- `make docker-run` - run the Docker image on port `8080`
