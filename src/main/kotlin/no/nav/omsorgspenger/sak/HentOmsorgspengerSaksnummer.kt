package no.nav.omsorgspenger.sak

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.CorrelationId.Companion.correlationId
import no.nav.omsorgspenger.sak.db.SaksnummerRepository
import org.slf4j.LoggerFactory

internal class HentOmsorgspengerSaksnummer(
    rapidsConnection: RapidsConnection,
    private val saksnummerRepository: SaksnummerRepository,
    private val hentIdentPdlMediator: HentIdentPdlMediator) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(HentOmsorgspengerSaksnummer::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(BEHOV)
                packet.require(IDENTITETSNUMMER) { it.requireArray { entry -> entry is TextNode } }
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Løser behov $BEHOV")

        val identitetsnummer = (packet[IDENTITETSNUMMER] as ArrayNode)
            .map { it.asText() }
            .toSet()

        val identitetsnummerForPersonerSomFinnes = runBlocking {
            hentIdentPdlMediator.hentIdentitetsnummer(identitetsnummer, packet.correlationId())
        }

        require(identitetsnummer == identitetsnummerForPersonerSomFinnes.keys) {
            "Mottatt behov for å opprette saksnummer på en eller flere personer som ikke finnes."
        }

        logger.info("Løser behovet for ${identitetsnummer.size} personer.")

        val saksnummer = identitetsnummerForPersonerSomFinnes
            .map { it.key to hentSaksnummerEllerLagNyttFor(it.key, it.value) }
            .toMap()

        packet.leggTilLøsning(BEHOV, mapOf(
            "saksnummer" to saksnummer
        ))
        return logger.info("Løst behøv $BEHOV med saksnummer ${saksnummer.values}").let { true }
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.info("Løst behov $BEHOV")
    }

    private fun hentSaksnummerEllerLagNyttFor(identitetsnummer: String, historiskIdent: Set<String>) =
        saksnummerRepository.hentSaksnummerEllerLagNytt(
            gjeldendeIdentitetsnummer = identitetsnummer,
            historiskeIdentitetsnummer = historiskIdent
        )

    internal companion object {
        const val BEHOV = "HentOmsorgspengerSaksnummer"
        internal val IDENTITETSNUMMER = "@behov.$BEHOV.identitetsnummer"
    }

}
