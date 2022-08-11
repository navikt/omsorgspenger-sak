package no.nav.omsorgspenger.sak.pdl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgspenger.CorrelationId
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI

internal class PdlClient(
    accessTokenClient: AccessTokenClient,
    private val scopes: Set<String>,
    private val pdlBaseUrl: URI,
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper
) : HealthCheck {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    suspend fun getPersonInfo(identitetsnummer: Set<String>, correlationId: CorrelationId): HentPdlBolkResponse {
        return httpClient.post(pdlBaseUrl.toString()) {
            header(HttpHeaders.Authorization, getAuthorizationHeader())
            header("Nav-Call-Id", "$correlationId")
            header("TEMA", "OMS")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(hentIdenterQuery(identitetsnummer))
        }.bodyAsText().also {
            secureLogger.info("PdlResponse=${JSONObject(it)}")
        }.let { objectMapper.readValue(it) }
    }

    private fun getAuthorizationHeader() = cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader()

    override suspend fun check() = kotlin.runCatching {
        httpClient.options(pdlBaseUrl.toString()) {
            header(HttpHeaders.Authorization, getAuthorizationHeader())
        }.status
    }.fold(
        onSuccess = { statusCode ->
            when (HttpStatusCode.OK == statusCode) {
                true -> Healthy("PdlClient", "OK")
                false -> UnHealthy("PdlClient", "Feil: Mottok Http Status Code ${statusCode.value}")
            }
        },
        onFailure = {
            UnHealthy("PdlClient", "Feil: ${it.message}")
        }
    )

    private companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
    }
}
