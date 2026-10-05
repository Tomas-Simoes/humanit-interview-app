# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY api/pom.xml .
COPY api/src ./src

RUN mvn -B clean package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system spring && adduser --system spring --ingroup spring

COPY --from=build /app/target/*.jar app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
