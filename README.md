omsorgspenger-sak
================

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

Dokumentation på integrasjoner i bruk:<br>
<a href="https://navikt.github.io/pdl/">PDL</a> slack: #mfn

