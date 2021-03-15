package no.nav.omsorgspenger

import io.ktor.application.*
import io.ktor.features.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behovsformat

internal data class CorrelationId private constructor(private val value: String) {
    override fun toString() = value
    internal companion object {
        internal fun ApplicationCall.correlationId() = CorrelationId(callId!!)
        internal fun JsonMessage.correlationId() = CorrelationId(get(Behovsformat.CorrelationId).asText())
    }
}