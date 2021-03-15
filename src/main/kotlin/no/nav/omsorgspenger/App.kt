package no.nav.omsorgspenger

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.auth.Issuer
import no.nav.helse.dusseldorf.ktor.auth.allIssuers
import no.nav.helse.dusseldorf.ktor.auth.multipleJwtIssuers
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.apis.SakApi
import no.nav.omsorgspenger.sak.HentOmsorgspengerSaksnummer
import java.net.URI
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.HealthReporter
import no.nav.k9.rapid.river.hentRequiredEnv

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

