package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.auth.Issuer
import no.nav.helse.dusseldorf.ktor.auth.allIssuers
import no.nav.helse.dusseldorf.ktor.auth.multipleJwtIssuers
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.apis.SakApi
import no.nav.omsorgspenger.sak.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.sak.db.DataSourceBuilder
import no.nav.omsorgspenger.sak.db.SaksnummerRepository
import no.nav.omsorgspenger.sak.db.migrate
import java.net.URI
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.core.DefaultStatusPages
import no.nav.helse.dusseldorf.ktor.health.HealthReporter
import javax.sql.DataSource
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.apis.TilgangsstyringRestClient
import no.nav.omsorgspenger.sak.pdl.PdlClient
import no.nav.omsorgspenger.config.ServiceUser
import no.nav.omsorgspenger.config.readServiceUserCredentials
import no.nav.omsorgspenger.sak.HentIdentPdlMediator

fun main() {
    val applicationContext = ApplicationContext.Builder().build()
    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(applicationContext.env))
        .withKtorModule { omsorgspengerSak(applicationContext) }
        .build()
        .apply { registerApplicationContext(applicationContext) }
        .start()
}

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    HentOmsorgspengerSaksnummer(
        rapidsConnection = this,
        saksnummerRepository = applicationContext.saksnummerRepository,
        hentIdentPdlMediator = applicationContext.hentIdentPdlMediator
    )
    register(object : RapidsConnection.StatusListener {
        override fun onStartup(rapidsConnection: RapidsConnection) {
            applicationContext.start()
        }
        override fun onShutdown(rapidsConnection: RapidsConnection) {
            applicationContext.stop()
        }
    })
}

internal fun Application.omsorgspengerSak(applicationContext: ApplicationContext) {
    install(ContentNegotiation) {
        jackson()
    }

    install(StatusPages) {
        DefaultStatusPages()
        AuthStatusPages()
    }

    val alias = "azure-v2"
    val azureV2 = Issuer(
        issuer = applicationContext.env.hentRequiredEnv("AZURE_V2_ISSUER"),
        jwksUri = URI(applicationContext.env.hentRequiredEnv("AZURE_V2_JWKS_URI")),
        audience = applicationContext.env.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
        alias = alias
    )

    val issuers = mapOf(alias to azureV2).withoutAdditionalClaimRules()

    install(Authentication) {
        multipleJwtIssuers(issuers)
    }

    HealthReporter(
        app = "omsorgspenger-sak",
        healthService = applicationContext.healthService
    )

    routing {
        HealthRoute(healthService = applicationContext.healthService)
        authenticate(*issuers.allIssuers()) {
            SakApi(
                saksnummerRepository = applicationContext.saksnummerRepository,
                hentIdentPdlMediator = applicationContext.hentIdentPdlMediator,
                tilgangsstyringRestClient = applicationContext.tilgangsstyringRestClient
            )
        }
    }
}

internal class ApplicationContext(
        val env: Environment,
        val dataSource: DataSource,
        val saksnummerRepository: SaksnummerRepository,
        val healthService: HealthService,
        val hentIdentPdlMediator: HentIdentPdlMediator,
        val tilgangsstyringRestClient: TilgangsstyringRestClient
) {

    internal fun start() {
        dataSource.migrate()
    }
    internal fun stop() {}

    internal class Builder(
            var env: Environment? = null,
            var dataSource: DataSource? = null,
            var saksnummerRepository: SaksnummerRepository? = null,
            var serviceUser: ServiceUser? = null,
            var httpClient: HttpClient? = null,
            var accessTokenClient: AccessTokenClient? = null,
            var pdlClient: PdlClient? = null,
            var hentIdentPdlMediator: HentIdentPdlMediator? = null,
            var tilgangsstyringRestClient: TilgangsstyringRestClient? = null
    ) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetHttpClient = httpClient ?: HttpClient {
                install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
            }
            val benyttetServiceUser = serviceUser ?: readServiceUserCredentials()
            val benyttetAccessTokenClient = accessTokenClient?: ClientSecretAccessTokenClient(
                    clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                    clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                    tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_APP_TOKEN_ENDPOINT"))
            )
            val benyttetPdlClient = pdlClient ?: PdlClient(
                    env = benyttetEnv,
                    accessTokenClient = benyttetAccessTokenClient,
                    serviceUser = benyttetServiceUser,
                    httpClient = benyttetHttpClient,
                    objectMapper = objectMapper
            )
            val benyttetHentIdentPdlMediator = hentIdentPdlMediator?: HentIdentPdlMediator(benyttetPdlClient)

            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()
            val benyttetSaksnummerRepository = saksnummerRepository ?: SaksnummerRepository(benyttetDataSource)

            val benyttetTilgangsstyringRestClient = tilgangsstyringRestClient ?: TilgangsstyringRestClient(
                httpClient = benyttetHttpClient,
                env = benyttetEnv
            )

            return ApplicationContext(
                env = benyttetEnv,
                dataSource = benyttetDataSource,
                saksnummerRepository = benyttetSaksnummerRepository,
                hentIdentPdlMediator = benyttetHentIdentPdlMediator,
                healthService = HealthService(
                    healthChecks = setOf(
                        benyttetSaksnummerRepository,
                        benyttetPdlClient
                    )
                ),
                tilgangsstyringRestClient = benyttetTilgangsstyringRestClient
            )
        }

        private companion object {
            val objectMapper: ObjectMapper = jacksonObjectMapper()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .registerModule(JavaTimeModule())
        }
    }
}
