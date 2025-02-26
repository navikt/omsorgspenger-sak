package no.nav.omsorgspenger.apis

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgspenger.CorrelationId
import org.slf4j.LoggerFactory
import java.net.URI

internal class TilgangsstyringRestClient(
    private val httpClient: HttpClient,
    private val accessTokenClient: AccessTokenClient,
    private val scopes: Set<String>,
    baseUrl: URI
) : HealthCheck {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient);
    private val tilgangUrl = "${baseUrl}/api/tilgang/personer"

    internal suspend fun sjekkTilgang(
        identitetsnummer: Set<String>,
        authorizationHeader: String,
        beskrivelse: String,
        correlationId: CorrelationId
    ): Boolean {
        return kotlin.runCatching {
            httpClient.post(tilgangUrl) {
                header(
                    HttpHeaders.Authorization, cachedAccessTokenClient.getAccessToken(
                        scopes = scopes,
                        onBehalfOf = authorizationHeader.removePrefix("Bearer ")
                    ).asAuthoriationHeader()
                )
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.XCorrelationId, "$correlationId")
                setBody(PersonerRequestBody(identitetsnummer, Operasjon.Visning, beskrivelse))
            }
        }.håndterResponse()
    }

    private suspend fun Result<HttpResponse>.håndterResponse(): Boolean = fold(
        onSuccess = { response ->
            when (response.status) {
                HttpStatusCode.NoContent -> true
                HttpStatusCode.Forbidden -> false
                else -> {
                    response.logError()
                    throw RuntimeException("Uventet response code (${response.status}) ved tilgangssjekk")
                }
            }
        },
        onFailure = { cause ->
            when (cause is ResponseException) {
                true -> {
                    cause.response.logError()
                    throw RuntimeException("Uventet feil ved tilgangssjekk")
                }
                else -> throw cause
            }
        }
    )

    private suspend fun HttpResponse.logError() =
        logger.error("HTTP ${status.value} fra omsorgspenger-tilgangsstyring, response: ${String(bodyAsText().toByteArray())}")

    override suspend fun check(): no.nav.helse.dusseldorf.ktor.health.Result {
        return no.nav.helse.dusseldorf.ktor.health.Result.merge(
            name = "TilgangsstyringRestClient",
            pingCheck(),
            accessTokenCheck()
        )
    }

    private suspend fun pingCheck(): no.nav.helse.dusseldorf.ktor.health.Result {
        return kotlin.runCatching {
            httpClient.get("$tilgangUrl/isalive").body<HttpStatement>().execute()
        }.fold(
            onSuccess = { response ->
                when (HttpStatusCode.OK == response.status) {
                    true -> Healthy("PingCheck", "OK")
                    false -> UnHealthy("PingCheck", "Feil: Mottok Http Status Code ${response.status.value}")
                }
            },
            onFailure = {
                UnHealthy("PingCheck", "Feil: ${it.message}")
            }
        )
    }

    private fun accessTokenCheck() = kotlin.runCatching {
        val accessTokenResponse = accessTokenClient.getAccessToken(scopes)
        (SignedJWT.parse(accessTokenResponse.accessToken).jwtClaimsSet.getStringArrayClaim("roles")?.toList()
            ?: emptyList()).contains("access_as_application")
    }.fold(
        onSuccess = {
            when (it) {
                true -> Healthy("AccessTokenCheck", "OK")
                false -> UnHealthy("AccessTokenCheck", "Feil: Mangler rettigheter")
            }
        },
        onFailure = { UnHealthy("AccessTokenCheck", "Feil: ${it.message}") }
    )

    private companion object {
        private val logger = LoggerFactory.getLogger(TilgangsstyringRestClient::class.java)

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
