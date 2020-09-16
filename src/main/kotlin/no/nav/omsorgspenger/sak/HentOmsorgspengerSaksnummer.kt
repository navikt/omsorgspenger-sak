package no.nav.omsorgspenger.sak

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class HentOmsorgspengerSaksnummer(rapidsConnection: RapidsConnection, dataSource: DataSource) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            validate {
                it.requireKey(Behov)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()
        logger.info("Skal løse behov $Behov med id $id")
    }

    internal companion object {
        const val Behov = "HentOmsorgspengerSaksnummer"
    }

}
