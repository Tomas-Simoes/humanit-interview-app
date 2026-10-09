# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY api/pom.xml .
COPY api/src ./src

RUN mvn -B clean package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --system spring \
    && adduser --system spring --ingroup spring

COPY --from=build /app/target/*.jar app.jar

ENV LOGGING_FILE_NAME=

USER spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
