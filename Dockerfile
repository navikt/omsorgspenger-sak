FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.04.24.0932Z
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-sak

COPY build/libs/app.jar /app/app.jar
WORKDIR /app
