# syntax=docker/dockerfile:1
# Build every application once; the cached build stage is shared by all 16 images.
# No Spring profile, database password or AWS credential is baked into an image.
ARG BUILD_IMAGE=eclipse-temurin:21-jdk-alpine
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-alpine
FROM ${BUILD_IMAGE} AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,id=pg-platform-gradle,target=/root/.gradle,sharing=locked \
    sh infra/docker/build-jars.sh

FROM ${RUNTIME_IMAGE} AS runtime
ARG SERVICE_NAME
ARG SERVICE_PORT=8080
RUN addgroup -S -g 10001 app && adduser -S -D -H -u 10001 -G app app
WORKDIR /app
COPY --from=build --chown=10001:10001 /artifacts/${SERVICE_NAME}.jar /app/app.jar
ENV SERVER_PORT=${SERVICE_PORT}
USER 10001:10001
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=12 \
    CMD wget -q -O /dev/null "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
