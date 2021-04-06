package no.nav.omsorgspenger.sak

import io.prometheus.client.Counter

internal object SakMetrics {

    private fun Counter.ensureRegistered() = try {
        register()
    } catch (cause: IllegalArgumentException) {
        this
    }

    private val sakTypBehandlet: Counter = Counter
        .build("omsorgspenger_sak_behandlet_total", "Typer av saker behandlet")
        .labelNames("typ")
        .create()
        .ensureRegistered()

    internal fun incHentSaksnummer() {
        sakTypBehandlet.labels("hent_sak").inc()
    }

    internal fun incNyttSaksnummer() {
        sakTypBehandlet.labels("ny_sak").inc()
    }

    internal fun incFannHistoriskSak() {
        sakTypBehandlet.labels("historisk_sak").inc()
    }
}