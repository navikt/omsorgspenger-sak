package no.nav.omsorgspenger.sak

import no.nav.omsorgspenger.client.pdl.Identitetsnummer
import no.nav.omsorgspenger.client.pdl.PdlClient

internal class HentIdentPdlMediator(
        internal val pdlClient: PdlClient
) {

    suspend fun hentIdentitetsnummer(identer: Set<Identitetsnummer>): Map<Identitetsnummer, Set<Identitetsnummer>> {
        try {
            val pdlResponse = pdlClient.getPersonInfo(identer)
            if (!pdlResponse.errors.isNullOrEmpty()) {
                throw IllegalStateException("Fick feil vid hent av data fra PDL: ${pdlResponse.errors}")
            }

            var losning  = mutableMapOf<Identitetsnummer, Set<Identitetsnummer>>()
            pdlResponse.data.hentIdenterBolk?.map { ident ->
                if (ident.code == "ok") {
                    losning[ident.ident] = ident.identer!!.map { it.ident }.toSet()
                }
            }
            return losning
        } catch (cause: Throwable) {
            throw IllegalStateException("Feil vid hent av data fra PDL:", cause)
        }
    }
}