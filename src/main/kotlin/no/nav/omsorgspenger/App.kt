package no.nav.omsorgspenger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.*
import no.nav.helse.dusseldorf.ktor.auth.Issuer
import no.nav.helse.dusseldorf.ktor.auth.allIssuers
import no.nav.helse.dusseldorf.ktor.auth.multipleJwtIssuers
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.omsorgspenger.apis.SakApi
import no.nav.omsorgspenger.sak.HentOmsorgspengerSaksnummer
import java.net.URI
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.*
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.k9.rapid.river.hentRequiredEnv

fun main() {
    val applicationContext = ApplicationContext.Builder().build()
    RapidApplication.create(
        env = applicationContext.env,
        builder = { withKtorModule { omsorgspengerSak(applicationContext) } }
    )
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
    register(RapidsStateListener(onStateChange = { state -> applicationContext.rapidsState = state }))
}

internal fun Application.omsorgspengerSak(applicationContext: ApplicationContext) {
    install(ContentNegotiation) {
        jackson()
    }

    install(StatusPages) {
        DefaultStatusPages()
        AuthStatusPages()
    }

    install(CallId) {
        fromFirstNonNullHeader(
            headers = listOf(HttpHeaders.XCorrelationId, "Nav-Call-Id"),
            generateOnNotSet = true
        )
    }

    install(CallLogging) {
        logRequests()
        correlationIdAndRequestIdInMdc()
    }

    val alias = "azure-v2"
    val azureV2 = Issuer(
        issuer = applicationContext.env.hentRequiredEnv("AZURE_OPENID_CONFIG_ISSUER"),
        jwksUri = URI(applicationContext.env.hentRequiredEnv("AZURE_OPENID_CONFIG_JWKS_URI")),
        audience = applicationContext.env.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
        alias = alias
    )

    val issuers = mapOf(alias to azureV2).withoutAdditionalClaimRules()

    install(Authentication) {
        multipleJwtIssuers(issuers)
    }

    val healthService = HealthService(
        healthChecks = applicationContext.healthChecks.plus(object : HealthCheck {
            override suspend fun check(): Result {
                val currentState = applicationContext.rapidsState
                return when (currentState.isHealthy()) {
                    true -> Healthy("RapidsConnection", currentState.asMap)
                    false -> UnHealthy("RapidsConnection", currentState.asMap)
                }
            }
        })
    )

    HealthReporter(
        app = "omsorgspenger-sak",
        healthService = healthService
    )

    routing {
        HealthRoute(healthService = healthService)
        authenticate(*issuers.allIssuers()) {
            SakApi(
                saksnummerRepository = applicationContext.saksnummerRepository,
                hentIdentPdlMediator = applicationContext.hentIdentPdlMediator,
                tilgangsstyringRestClient = applicationContext.tilgangsstyringRestClient
            )
        }
    }
}

