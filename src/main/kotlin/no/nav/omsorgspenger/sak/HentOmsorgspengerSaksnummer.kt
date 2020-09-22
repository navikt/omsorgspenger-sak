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
    val saksnummerRepository = SaksnummerRepository(dataSource)

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(Behov)
                it.require(identitetsnummer, JsonNode::requireText)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()
        logger.info("Skal løse behov $Behov med id $id")

        lateinit var saksnummer: String
        try {
            saksnummer = saksnummerRepository.hentSaksnummer(packet[identitetsnummer].asText())
        } catch (throwable: Throwable) {
            logger.error("Uh uh = ${throwable.message}")
            saksnummer = id.substring(0, 9)
        }
        val løsning = mapOf("saksnummer" to saksnummer)

        packet.leggTilLøsning(Behov, løsning)
        logger.info("Løst behøv: $Behov med saksnummer: ${løsning.toString()}")
        context.sendMedId(packet)

    }


    internal companion object {
        const val Behov = "HentOmsorgspengerSaksnummer"
        internal val identitetsnummer = "@behov.$Behov.identitetsnummer"
    }

}