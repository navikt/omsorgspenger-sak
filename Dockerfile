FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.01.15.0735z
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-sak

COPY build/libs/app.jar /app/app.jar
WORKDIR /app
CMD [ "app.jar" ]
