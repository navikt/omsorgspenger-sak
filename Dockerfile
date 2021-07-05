FROM navikt/java:16
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-sak
COPY build/libs/app.jar .
