package no.nav.omsorgspenger.apis

import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
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
    hentIdentPdlMediator: HentIdentPdlMediator,
    tilgangsstyringRestClient: TilgangsstyringRestClient
) {
    post("/saksnummer") {
        val identitetsnummer = call.receive<HentSaksnummerRequestBody>().identitetsnummer

        val authHeader = call.request.headers[HttpHeaders.Authorization]
            ?: return@post call.respond(HttpStatusCode.Unauthorized)

        val identer = setOf(identitetsnummer)
        val beskrivelse = "slå opp saksnummer"
        val harTilgangTilSaksnummer = tilgangsstyringRestClient.sjekkTilgang(identer, authHeader, beskrivelse)

        if (!harTilgangTilSaksnummer) {
            return@post call.respond(HttpStatusCode.Forbidden)
        }

        val saksnummer = saksnummerRepository.hentSaksnummer(identer)
            ?: saksnummerRepository.hentSaksnummer(
                hentIdentPdlMediator.hentIdentitetsnummer(identer).getOrDefault(identitetsnummer, emptySet())
            )

        if (saksnummer != null)
            call.respond(HentSaksnummerResponseBody(saksnummer))
        else
            call.respond(HttpStatusCode.NotFound)
    }
}
