package no.nav.omsorgspenger.sak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.requireText
import no.nav.k9.rapid.river.sendMedId
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.sak.db.SaksnummerRepository
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class HentOmsorgspengerSaksnummer(
        rapidsConnection: RapidsConnection,
        dataSource: DataSource) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val saksnummerRepository = SaksnummerRepository(dataSource)

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(BEHOV)
                it.require(IDENTITETSNUMMER, JsonNode::requireText)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()
        logger.info("Skal løse behov $BEHOV med id $id")

        val saksnummer = saksnummerRepository.hentSaksnummer(packet[IDENTITETSNUMMER].asText())
        val løsning = mapOf("saksnummer" to saksnummer)

        packet.leggTilLøsning(BEHOV, løsning)
        logger.info("Løst behøv: $BEHOV $id med saksnummer: ${løsning.toString()}")
        context.sendMedId(packet)

    }


    internal companion object {
        const val BEHOV = "HentOmsorgspengerSaksnummer"
        internal val IDENTITETSNUMMER = "@behov.$BEHOV.identitetsnummer"
    }

}