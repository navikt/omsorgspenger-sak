package no.nav.omsorgspenger.client

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.omsorgspenger.config.Environment
import no.nav.omsorgspenger.config.hentRequiredEnv
import org.slf4j.LoggerFactory

// TODO: health check
internal class TilgangsstyringRestClient(
    private val httpClient: HttpClient,
    env: Environment
) {

    private val logger = LoggerFactory.getLogger(TilgangsstyringRestClient::class.java)
    private val tilgangUrl = env.hentRequiredEnv("TILGANGSSTYRING_URL")

    internal suspend fun sjekkTilgang(identer: Set<String>, authHeader: String, beskrivelse: String): Boolean {
        val response = httpClient.post<HttpStatement>("$tilgangUrl/api/tilgang/personer") {
            header(HttpHeaders.Authorization, authHeader)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            body = PersonerRequestBody(identer, Operasjon.Visning, beskrivelse)
        }.execute()

        return when (response.status) {
            HttpStatusCode.NoContent -> true
            HttpStatusCode.Forbidden -> false
            else -> {
                logger.error("Uventet response code ved tilgangssjekk", response.status, response.readText())
                throw RuntimeException("Uventet response code (${response.status}) ved tilgangssjekk")
            }
        }
    }
}

enum class Operasjon {
    Visning
}

data class PersonerRequestBody(
    val identitetsnummer: Set<String>,
    val operasjon: Operasjon,
    val beskrivelse: String
)
