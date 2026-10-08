# TODO

## Production-readiness follow-ups

- [ ] Validation and normalization
  - Trim string inputs before persistence.
  - Normalize emails consistently before uniqueness checks and login.
  - Enforce case-insensitive email uniqueness.
  - Add business validation for tax identifiers and phone numbers.
  - Add document expiration rules, such as rejecting already-expired documents if that matches the domain.

- [ ] Observability
  - Add structured application logs.
  - Add request/correlation IDs to logs and error responses.
  - Add Spring Boot Actuator health and readiness endpoints.
  - Add metrics for API latency, errors, and authentication failures.
  - Keep production log levels quiet by default and avoid SQL logging outside local debugging.

- [ ] API contract and documentation
  - Replace `ResponseEntity<?>` in `GET /api/v1/clients` with a concrete response type.
  - Introduce explicit pagination response DTOs instead of exposing ambiguous or framework-specific response shapes.
  - Update OpenAPI documentation after the response contracts are made concrete.

- [ ] Docker and deployment
  - Document Docker build and run commands in the README.
  - Pass runtime configuration through environment variables.
  - Add a container healthcheck.
  - Ensure CI runs tests before building the Docker image.

- [ ] Static analysis and coverage
  - Add a formatter such as Spotless or Checkstyle.
  - Add static analysis with SpotBugs or PMD.
  - Add JaCoCo coverage reporting.
  - Wire formatting, static analysis, tests, and coverage into the Maven verification lifecycle or CI.


Remove Schema from DTOs