# --- build ---
# Official images from the ECR Public mirror: Docker Hub rate-limits shared CI addresses.
ARG REGISTRY=public.ecr.aws/docker/library
FROM ${REGISTRY}/maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# --- run ---
ARG REGISTRY=public.ecr.aws/docker/library
FROM ${REGISTRY}/eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /src/target/store-api-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
