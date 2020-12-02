package no.nav.omsorgspenger.sak

import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

private object SakMetrics {

    val logger = LoggerFactory.getLogger(SakMetrics::class.java)

    val mottattBehov: Counter = Counter
            .build("omsorgspenger_behov_mottatt_total", "Antal behov mottatt")
            .labelNames("behov")
            .register()

    val feilBehovBehandling: Counter = Counter
            .build("omsorgspenger_behov_feil_total", "Antal feil vid behandling av behov")
            .labelNames("behov")
            .register()

    val behovBehandlet: Counter = Counter
            .build("omsorgspenger_behov_behandlet_total", "Antal lyckade behandlinger av behov")
            .labelNames("behov")
            .register()

    val sakTypBehandlet: Counter = Counter
            .build("omsorgspenger_sak_behandlet_total", "Typer av saker behandlet")
            .labelNames("typ")
            .register()
}

private fun safeMetric(block: () -> Unit) = try {
    block()
} catch (cause: Throwable) {
    SakMetrics.logger.warn("Feil ved å rapportera metrics", cause)
}

internal fun incPostgresFeil() {
    safeMetric { SakMetrics.feilBehovBehandling.labels("postgres_feil").inc() }
}

internal fun incLostBehov(behov: String) {
    safeMetric { SakMetrics.behovBehandlet.labels(behov).inc() }
}

internal fun incMottattBehov(behov: String) {
    safeMetric { SakMetrics.mottattBehov.labels(behov).inc() }
}

internal fun incHentSaksnummer() {
    safeMetric { SakMetrics.sakTypBehandlet.labels("hent_sak").inc() }
}

internal fun incNyttSaksnummer() {
    safeMetric { SakMetrics.sakTypBehandlet.labels("ny_sak").inc() }
}

internal fun incFannHistoriskSak() {
    safeMetric { SakMetrics.sakTypBehandlet.labels("historisk_sak").inc() }
}