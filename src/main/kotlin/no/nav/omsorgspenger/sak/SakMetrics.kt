package no.nav.omsorgspenger.sak

import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

private object SakMetrics {

    val logger = LoggerFactory.getLogger(this::class.java)

    val postgresFeil: Counter = Counter
            .build("postgres_feil", "Feil vid postgres-kall")
            .register()

    val lostBehov: Counter = Counter
            .build("lost_behov", "Løst behov")
            .register()

    val mottattBehov: Counter = Counter
            .build("mottatt_behov", "Mottatt behov")
            .register()

    val hentSaksnummer: Counter = Counter
            .build("hent_saksnummer", "Hæmtar existerande saksnummer")
            .register()

    val nyttSaksnummer: Counter = Counter
            .build("nytt_saksnummer", "Genererar nytt saksnummer")
            .register()
}

private fun safeMetric(block: () -> Unit) = try {
    block()
} catch (cause: Throwable) {
    SakMetrics.logger.warn("Feil ved å rapportera metrics", cause)
}

internal fun incPostgresFeil() {
    safeMetric { SakMetrics.postgresFeil.inc() }
}

internal fun incLostBehov() {
    safeMetric { SakMetrics.lostBehov.inc() }
}

internal fun incMottattBehov() {
    safeMetric { SakMetrics.mottattBehov.inc() }
}

internal fun incHentSaksnummer() {
    safeMetric { SakMetrics.hentSaksnummer.inc() }
}

internal fun incNyttSaksnummer() {
    safeMetric { SakMetrics.nyttSaksnummer.inc() }
}