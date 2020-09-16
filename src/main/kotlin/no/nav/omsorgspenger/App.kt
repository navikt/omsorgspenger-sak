package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.sak.HentOmsorgspengerSaksnummer

fun main() {

    val env = System.getenv()

    val dataSourceBuilder = DataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource()

    RapidApplication.create(env).apply {
        HentOmsorgspengerSaksnummer(this, dataSource)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                // migrate database before consuming messages, but after rapids have started (and isalive returns OK)
                dataSourceBuilder.migrate()
            }
        })
    }.start()
}