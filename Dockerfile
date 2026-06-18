# syntax=docker/dockerfile:1

# 1) Build the React front-end.
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) Build the Spring Boot jar, bundling the front-end as static resources so the
#    app serves the UI and the API from one origin.
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY src ./src
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -q -B clean package -DskipTests

# 3) Slim runtime image.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/target/spring-mcp-agent-0.1.0.jar app.jar
# Render injects $PORT; the app reads it via application.yml.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
