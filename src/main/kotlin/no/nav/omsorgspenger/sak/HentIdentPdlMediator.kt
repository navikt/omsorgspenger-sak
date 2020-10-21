package no.nav.omsorgspenger.sak

import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.client.pdl.Ident
import no.nav.omsorgspenger.client.pdl.PdlClient
import org.slf4j.LoggerFactory

internal class HentIdentPdlMediator(
        internal val pdlClient: PdlClient
) {
    private val secureLogger = LoggerFactory.getLogger("tjenestekall")

    fun hentIdenter(identer: Set<String>): Map<String, Set<String>> {
        var losning = mutableMapOf<String, Set<String>>()
        runBlocking {
            try {
                val response = pdlClient.getPersonInfo(identer)
                if (!response.errors.isNullOrEmpty()) {
                    secureLogger.error("Fann feil vid hent av data fra PDL:", response.errors)
                }
                response.data.hentIdenterBolk?.forEach { it ->
                    var identSet = mutableSetOf<String>()
                    it.identer?.forEach { identSet.add(it.ident) }
                    losning.put(it.ident.toString(), identSet)
                }
            } catch (cause: Throwable) {
                throw IllegalStateException("Feil vid hent av data fra PDL:", cause)
            }
        }

        return losning.toMap()
    }
}