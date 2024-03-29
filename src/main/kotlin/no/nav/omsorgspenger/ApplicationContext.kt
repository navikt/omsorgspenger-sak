package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.k9.rapid.river.csvTilSet
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.apis.TilgangsstyringRestClient
import no.nav.omsorgspenger.sak.HentIdentPdlMediator
import no.nav.omsorgspenger.sak.db.DataSourceBuilder
import no.nav.omsorgspenger.sak.db.SaksnummerRepository
import no.nav.omsorgspenger.sak.db.migrate
import no.nav.omsorgspenger.sak.pdl.PdlClient
import java.net.URI
import javax.sql.DataSource

internal class ApplicationContext(
    val env: Environment,
    val dataSource: DataSource,
    val saksnummerRepository: SaksnummerRepository,
    val healthChecks: Set<HealthCheck>,
    val hentIdentPdlMediator: HentIdentPdlMediator,
    val tilgangsstyringRestClient: TilgangsstyringRestClient
) {
    internal var rapidsState = RapidsStateListener.RapidsState.initialState()

    internal fun start() {
        dataSource.migrate()
    }

    internal fun stop() {}

    internal class Builder(
        var env: Environment? = null,
        var dataSource: DataSource? = null,
        var saksnummerRepository: SaksnummerRepository? = null,
        var httpClient: HttpClient? = null,
        var accessTokenClient: AccessTokenClient? = null,
        var pdlClient: PdlClient? = null,
        var hentIdentPdlMediator: HentIdentPdlMediator? = null,
        var tilgangsstyringRestClient: TilgangsstyringRestClient? = null
    ) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetHttpClient = httpClient ?: HttpClient(CIO) {
                install(ContentNegotiation) {
                    jackson()
                }
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 2)
                    delayMillis {attempt ->
                        when (attempt) {
                            1 -> 500
                            2 -> 2000
                            else -> 5000
                        }

                    }
                }
                expectSuccess = false
            }
            val benyttetAccessTokenClient = accessTokenClient ?: ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")),
                authenticationMode = ClientSecretAccessTokenClient.AuthenticationMode.POST
            )
            val benyttetPdlClient = pdlClient ?: PdlClient(
                accessTokenClient = benyttetAccessTokenClient,
                httpClient = benyttetHttpClient,
                objectMapper = objectMapper,
                pdlBaseUrl = URI(benyttetEnv.hentRequiredEnv("PDL_BASE_URL")),
                scopes = benyttetEnv.hentRequiredEnv("PDL_SCOPES").csvTilSet()
            )
            val benyttetHentIdentPdlMediator = hentIdentPdlMediator ?: HentIdentPdlMediator(benyttetPdlClient)

            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()
            val benyttetSaksnummerRepository = saksnummerRepository ?: SaksnummerRepository(benyttetDataSource)

            val benyttetTilgangsstyringRestClient = tilgangsstyringRestClient ?: TilgangsstyringRestClient(
                httpClient = benyttetHttpClient,
                baseUrl = URI(benyttetEnv.hentRequiredEnv("OMSORGSPENGER_TILGANGSSTYRING_BASE_URL")),
                scopes = benyttetEnv.hentRequiredEnv("OMSORGSPENGER_TILGANGSSTYRING_SCOPES").csvTilSet(),
                accessTokenClient = benyttetAccessTokenClient
            )

            return ApplicationContext(
                env = benyttetEnv,
                dataSource = benyttetDataSource,
                saksnummerRepository = benyttetSaksnummerRepository,
                hentIdentPdlMediator = benyttetHentIdentPdlMediator,
                healthChecks = setOf(
                    benyttetSaksnummerRepository,
                    benyttetPdlClient
                ),
                tilgangsstyringRestClient = benyttetTilgangsstyringRestClient
            )
        }

        private companion object {
            val objectMapper: ObjectMapper = jacksonObjectMapper()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(JavaTimeModule())
        }
    }
}
