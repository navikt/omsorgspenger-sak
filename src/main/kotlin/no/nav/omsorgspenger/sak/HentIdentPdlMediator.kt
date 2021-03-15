package no.nav.omsorgspenger.sak

import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.sak.pdl.Identitetsnummer
import no.nav.omsorgspenger.sak.pdl.PdlClient

internal class HentIdentPdlMediator(
    private val pdlClient: PdlClient) {

    internal suspend fun hentIdentitetsnummer(
        identitetsnummer: Set<Identitetsnummer>,
        correlationId: CorrelationId): Map<Identitetsnummer, Set<Identitetsnummer>> {

        try {
            val pdlResponse = pdlClient.getPersonInfo(identitetsnummer, correlationId)
            if (!pdlResponse.errors.isNullOrEmpty()) {
                throw IllegalStateException("Fick feil vid hent av data fra PDL: ${pdlResponse.errors}")
            } else if(pdlResponse.data.hentIdenterBolk.isNullOrEmpty()) {
                return emptyMap()
            }

            return pdlResponse.data.hentIdenterBolk
                    .filter { it.code == "ok" }
                    .map { it -> it.ident to (it.identer
                            ?.map { it.ident }
                            ?.toSet()?: setOf()) }
                    .toMap()

        } catch (cause: Throwable) {
            throw IllegalStateException("Feil vid hent av data fra PDL:", cause)
        }
    }
}