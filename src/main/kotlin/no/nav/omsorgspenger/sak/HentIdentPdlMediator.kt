package no.nav.omsorgspenger.sak

import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.client.pdl.PdlClient

internal class HentIdentPdlMediator(
        internal val pdlClient: PdlClient
) {

    fun hentIdenter(identer: Set<String>): Map<String, Set<String>> {
        var losning = mutableMapOf<String, Set<String>>()
        runBlocking {
            try {
                val response = pdlClient.getPersonInfo(identer)
                if (!response.errors.isNullOrEmpty()) {
                    throw IllegalStateException("Fick feil vid hent av data fra PDL: ${response.errors}")
                }
                response.data.hentIdenterBolk?.forEach { it ->
                    if(it.code.equals("ok")) {
                        var identSet = mutableSetOf<String>()
                        it.identer?.forEach { identSet.add(it.ident) }
                        losning[it.ident] = identSet
                    }
                }
            } catch (cause: Throwable) {
                throw IllegalStateException("Feil vid hent av data fra PDL:", cause)
            }
        }

        return losning.toMap()
    }
}