package no.nav.omsorgspenger.apis

import io.ktor.application.call
import io.ktor.http.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.omsorgspenger.client.pdl.Identitetsnummer
import no.nav.omsorgspenger.sak.HentIdentPdlMediator
import no.nav.omsorgspenger.sak.db.SaksnummerRepository

data class HentSaksnummerRequestBody(
        val identitetsnummer: String
)

data class HentSaksnummerResponseBody(
        val saksnummer: String
)

internal fun Route.SakApi(
        saksnummerRepository: SaksnummerRepository,
        hentIdentPdlMediator: HentIdentPdlMediator
) {
    post("/saksnummer") {
        val identitetsnummer = call.receive<HentSaksnummerRequestBody>().identitetsnummer
        val identer = setOf(identitetsnummer)
        val saksnummer = saksnummerRepository.hentSaksnummer(identer)
                ?: saksnummerRepository.hentSaksnummer(
                        hentIdentPdlMediator.hentIdentitetsnummer(identer).getOrDefault(identitetsnummer, emptySet()))

        if (saksnummer != null)
            call.respond(HentSaksnummerResponseBody(saksnummer))
        else
            call.respond(HttpStatusCode.NotFound)
    }
}
