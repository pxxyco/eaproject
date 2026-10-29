# Build Java 21 sources in a separate stage; no local JDK/Maven is required.
FROM maven:3.9-eclipse-temurin-21 AS build
ENV MAVEN_OPTS="-Xmx512m -XX:ActiveProcessorCount=2"
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -Prelease-dist -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
LABEL org.opencontainers.image.title="EA Project Backend" \
    org.opencontainers.image.version="1.0.0" \
    org.opencontainers.image.licenses="GPL-3.0-only" \
    org.opencontainers.image.source="https://github.com/pxxyco/untitled_ea_project"
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app \
    && mkdir -p /app/storage/photo /app/storage/private-chat /app/logs \
    && chown -R app:app /app
WORKDIR /app
# Demo photos seed the media volume the first time it is created.
COPY --chown=app:app demo-photo/ /app/storage/photo/
COPY --from=build --chown=app:app /build/target/release-dist/ea-project-1.0.0.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=container \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:ActiveProcessorCount=2 -Djava.awt.headless=true -Duser.timezone=Europe/Rome"
USER 10001:10001
EXPOSE 8080
STOPSIGNAL SIGTERM
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=4 \
    CMD if [ "$SERVER_SSL_ENABLED" = "true" ]; then wget --no-check-certificate -q -O /dev/null https://127.0.0.1:8080/api/health; else wget -q -O /dev/null http://127.0.0.1:8080/api/health; fi
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
