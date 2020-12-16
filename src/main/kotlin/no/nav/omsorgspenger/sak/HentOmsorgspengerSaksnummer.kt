package no.nav.omsorgspenger.sak

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
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
        logger.info("Løser behov $BEHOV").also { incMottattBehov(BEHOV) }

        val identitetsnummer = (packet[IDENTITETSNUMMER] as ArrayNode)
            .map { it.asText() }
            .toSet()

        val identerForFolkSomFinnes = runBlocking {
            hentIdentPdlMediator.hentIdentitetsnummer(identitetsnummer)
        }

        logger.info("Løser behovet for ${identitetsnummer.size} personer.")

        val saksnummer = identerForFolkSomFinnes
            .map { it.key to hentSaksnummerEllerLagNyttFor(it.key, it.value) }
            .toMap()

        packet.leggTilLøsning(BEHOV, mapOf(
            "saksnummer" to saksnummer
        ))
        return logger.info("Løst behøv $BEHOV med saksnummer ${saksnummer.values}").let { true }
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.info("Løst behov $BEHOV").also { incLostBehov(BEHOV) }
    }

    private fun hentSaksnummerEllerLagNyttFor(identitetsnummer: String, historiskIdent: Set<String>) = try {
            saksnummerRepository.hentSaksnummerEllerLagNytt(
                gjeldendeIdentitetsnummer = identitetsnummer,
                historiskeIdentitetsnummer = historiskIdent
            )
        } catch (cause: Throwable) {
            incPostgresFeil()
            throw cause
        }

    internal companion object {
        const val BEHOV = "HentOmsorgspengerSaksnummer"
        internal val IDENTITETSNUMMER = "@behov.$BEHOV.identitetsnummer"
    }

}
