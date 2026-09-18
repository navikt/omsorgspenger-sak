FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.09.16.0815Z
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-sak

COPY build/libs/app.jar /app/app.jar
WORKDIR /app
