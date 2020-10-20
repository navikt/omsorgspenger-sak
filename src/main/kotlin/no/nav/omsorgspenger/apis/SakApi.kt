package no.nav.omsorgspenger.apis

import io.ktor.application.call
import io.ktor.http.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.omsorgspenger.sak.db.SaksnummerRepository

data class HentSaksnummerRequestBody(
    val identitetsnummer: String
)

data class HentSaksnummerResponseBody(
    val saksnummer: String
)

internal fun Route.SakApi(
    saksnummerRepository: SaksnummerRepository
) {
    post("/saksnummer") {
        val identitetsnummer = call.receive<HentSaksnummerRequestBody>().identitetsnummer

        val saksnummer = saksnummerRepository.hentSaksnummer(identitetsnummer)

        if (saksnummer != null)
            call.respond(HentSaksnummerResponseBody(saksnummer))
        else
            call.respond(HttpStatusCode.NotFound)
    }
}
