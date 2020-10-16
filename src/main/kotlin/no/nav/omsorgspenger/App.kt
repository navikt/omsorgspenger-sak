package no.nav.omsorgspenger

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.sak.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.sak.db.DataSourceBuilder
import no.nav.omsorgspenger.sak.db.SaksnummerRepository
import no.nav.omsorgspenger.sak.db.migrate
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
    routing {
        HealthRoute(healthService = applicationContext.healthService)
    }
}

internal class ApplicationContext(
    val env: Environment,
    val dataSource: DataSource,
    val saksnummerRepository: SaksnummerRepository,
    val healthService: HealthService) {

    internal fun start() {
        dataSource.migrate()
    }
    internal fun stop() {}

    internal class Builder(
        var env: Environment? = null,
        var dataSource: DataSource? = null,
        var saksnummerRepository: SaksnummerRepository? = null) {
        internal fun build() : ApplicationContext {
            val benyttetEnv = env?:System.getenv()
            val benyttetDataSource = dataSource?:DataSourceBuilder(benyttetEnv).build()
            val benyttetSaksnummerRepository = saksnummerRepository?:SaksnummerRepository(benyttetDataSource)
            return ApplicationContext(
                env = benyttetEnv,
                dataSource = benyttetDataSource,
                saksnummerRepository = benyttetSaksnummerRepository,
                healthService = HealthService(healthChecks = setOf(
                    benyttetSaksnummerRepository
                ))
            )
        }
    }
}