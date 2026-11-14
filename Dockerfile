# syntax=docker/dockerfile:1.7
# ----------------------------------------------------------------------------
# Build: compila a imagem nativa (GraalVM) — inicialização em milissegundos e
# baixo consumo de memória, ideal para o plano gratuito do Render (512 MB).
# ----------------------------------------------------------------------------
FROM ghcr.io/graalvm/native-image-community:25 AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY hae-domain/pom.xml hae-domain/
COPY hae-spring/pom.xml hae-spring/
COPY hae-domain/src hae-domain/src
COPY hae-spring/src hae-spring/src

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -Pnative -DskipTests -Dnative.linking=--static-nolibc package

# ----------------------------------------------------------------------------
# Runtime: distroless (sem shell nem gerenciador de pacotes), usuário não-root.
# O binário é "mostly static": depende apenas da glibc.
# ----------------------------------------------------------------------------
FROM gcr.io/distroless/base-debian13:nonroot
LABEL org.opencontainers.image.title="hae-api" \
      org.opencontainers.image.description="API do Sistema de HAEs (Fatec/CPS) — Spring Boot nativo" \
      org.opencontainers.image.source="https://github.com/nickolss/back-hae" \
      org.opencontainers.image.licenses="MIT"

WORKDIR /app
COPY --from=build --chown=nonroot:nonroot /workspace/hae-spring/target/hae-api /app/hae-api

USER nonroot:nonroot
ENV PORT=8080
EXPOSE 8080

# Limita o heap a 70% da memória do contêiner (flag da imagem nativa, não da JVM).
ENTRYPOINT ["/app/hae-api", "-XX:MaximumHeapSizePercent=70"]
