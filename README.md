# HumanIT Interview App

Spring Boot API for managing clients and their documents. The service uses Java
21, Maven, Spring Security with JWT authentication, Flyway migrations, and an
in-memory H2 database.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker, optional, for container builds
- `make`, optional, for the shortcut commands below

## Run Locally

From the repository root:

```sh
make run
```

The API starts on `http://localhost:8080`.

Default local configuration is in
`api/src/main/resources/application.properties`. It uses an in-memory H2
database, so data is reset when the application stops.

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

The container exposes the API at `http://localhost:8080`.

## Make Targets

- `make run` - start the Spring Boot application
- `make compile` - compile the API
- `make test` - run unit tests
- `make verify` - run the Maven verification lifecycle
- `make build` - clean and package the jar
- `make clean` - remove Maven build output
- `make docker-build` - build the Docker image
- `make docker-run` - run the Docker image on port `8080`
