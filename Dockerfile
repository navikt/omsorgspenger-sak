FROM gcr.io/distroless/java21-debian12:nonroot
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-sak

COPY build/libs/app.jar /app/app.jar
WORKDIR /app
CMD [ "app.jar" ]
