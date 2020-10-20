package no.nav.omsorgspenger

import io.ktor.application.*
import io.ktor.auth.*
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
import javax.sql.DataSource

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
        saksnummerRepository = applicationContext.saksnummerRepository
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

    routing {
        HealthRoute(healthService = applicationContext.healthService)
        authenticate(*issuers.allIssuers()) {
            SakApi(
                saksnummerRepository = applicationContext.saksnummerRepository
            )
        }
    }
}

internal class ApplicationContext(
    val env: Environment,
    val dataSource: DataSource,
    val saksnummerRepository: SaksnummerRepository,
    val healthService: HealthService
) {

    internal fun start() {
        dataSource.migrate()
    }
    internal fun stop() {}

    internal class Builder(
        var env: Environment? = null,
        var dataSource: DataSource? = null,
        var saksnummerRepository: SaksnummerRepository? = null
    ) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()
            val benyttetSaksnummerRepository = saksnummerRepository ?: SaksnummerRepository(benyttetDataSource)
            return ApplicationContext(
                env = benyttetEnv,
                dataSource = benyttetDataSource,
                saksnummerRepository = benyttetSaksnummerRepository,
                healthService = HealthService(
                    healthChecks = setOf(
                        benyttetSaksnummerRepository
                    )
                )
            )
        }
    }
}
