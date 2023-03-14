omsorgspenger-sak
================
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=navikt_omsorgspenger-sak&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=navikt_omsorgspenger-sak)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=navikt_omsorgspenger-sak&metric=security_rating)](https://sonarcloud.io/summary/overall?id=navikt_omsorgspenger-sak)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=navikt_omsorgspenger-sak&metric=ncloc)](https://sonarcloud.io/summary/overall?id=navikt_omsorgspenger-sak)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=navikt_omsorgspenger-sak&metric=duplicated_lines_density)](https://sonarcloud.io/summary/overall?id=navikt_omsorgspenger-sak)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=navikt_omsorgspenger-sak&metric=coverage)](https://sonarcloud.io/summary/overall?id=navikt_omsorgspenger-sak)

Kafka-tjänst som bygger på <a href="https://github.com/navikt/k9-rapid">k9-rapid</a>.

### HentOmsorgspengerSaksnummer ###
* `Behov`: HentOmsorgspengerSaksnummer
* `Integrasjoner`: PDL, Postgres
* `Løsningsbeskrivelse`: Sjekker så att identitetsnummer i request finnes i PDL. 
For personer som finnes hentes eller genereres nytt saksnummer (der som det ikke finnes historiskt).
Alle saksnummer lagras i database.
* `JSON exempel behov`:
```
"@behov":{
   "HentOmsorgspengerSaksnummer":{
      "identitetsnummer":[
         "11111111111"
      ]
   }
}
```
* `JSON exempel løsning`:
```
"@løsninger":{
      "HentOmsorgspengerSaksnummer":{
         "saksnummer":{
            "11111111111":"TEST12345"
         },
         "løst":"2021-09-21T11:34:39.801Z"
      }
   }
```

### REST API: POST /saksnummer ###
Identitetsnummer sendes som String i body, API returnerer saksnummer bundet til identitetsnummer.  
Krever bearer token, tilgangstyring er implementert m.h.a. <a href="https://github.com/navikt/omsorgspenger-tilgangsstyring">omsorgspenger-tilgangsstyring</a>.

---

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #sif_omsorgspenger.
