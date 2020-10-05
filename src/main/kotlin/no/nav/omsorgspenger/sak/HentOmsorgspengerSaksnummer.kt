package no.nav.omsorgspenger.sak

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.sak.db.SaksnummerRepository
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class HentOmsorgspengerSaksnummer(
        rapidsConnection: RapidsConnection,
        dataSource: DataSource) : BehovssekvensPacketListener(
        logger = LoggerFactory.getLogger(HentOmsorgspengerSaksnummer::class.java)) {

    private val saksnummerRepository = SaksnummerRepository(dataSource)

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(BEHOV)
                packet.require(IDENTITETSNUMMER) { it.requireArray { entry -> entry is TextNode } }
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Løser behov $BEHOV").also { incMottattBehov() }

        val identitetsnummer = (packet[IDENTITETSNUMMER] as ArrayNode)
            .map { it.asText() }
            .toSet()

        logger.info("Løser behovet for ${identitetsnummer.size} personer.")

        val saksnummer = identitetsnummer
            .map { it to hentSaksnummerFor(it) }
            .toMap()
            .also { require(it.size == identitetsnummer.size) }
            .also { require(it.keys.containsAll(identitetsnummer)) }

        packet.leggTilLøsning(BEHOV, mapOf(
            "saksnummer" to saksnummer
        ))
        return logger.info("Løst behøv $BEHOV med saksnummer ${saksnummer.values}").let { true }
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.info("Løst behov $BEHOV").also { incLostBehov() }
    }

    private fun hentSaksnummerFor(identitetsnummer: String) = try {
            saksnummerRepository.hentSaksnummer(identitetsnummer)
        } catch (cause: Throwable) {
            incPostgresFeil()
            throw cause
        }

    internal companion object {
        const val BEHOV = "HentOmsorgspengerSaksnummer"
        internal val IDENTITETSNUMMER = "@behov.$BEHOV.identitetsnummer"
    }

}