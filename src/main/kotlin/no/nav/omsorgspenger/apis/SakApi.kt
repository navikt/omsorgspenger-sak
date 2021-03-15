package no.nav.omsorgspenger.apis

import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.CorrelationId.Companion.correlationId
import no.nav.omsorgspenger.sak.HentIdentPdlMediator
import no.nav.omsorgspenger.sak.db.SaksnummerRepository
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.omsorgspenger.apis.SakApi")

data class HentSaksnummerRequestBody(
    val identitetsnummer: String
)

data class HentSaksnummerResponseBody(
    val saksnummer: String
)

internal fun Route.SakApi(
    saksnummerRepository: SaksnummerRepository,
    hentIdentPdlMediator: HentIdentPdlMediator,
    tilgangsstyringRestClient: TilgangsstyringRestClient) {

    suspend fun harTilgangTilSaksnummer(
        authorizationHeader: String,
        correlationId: CorrelationId,
        jwtPrincipal: JWTPrincipal,
        identitetsnummer: Set<String>) : Boolean {

        val tilgangSomSystem = (jwtPrincipal.payload.getClaim("roles").asList(String::class.java)?: emptyList())
            .contains("access_as_application")

        return when (tilgangSomSystem) {
            true -> true.also { logger.info("Har tilgang som applikasjon.") }
            false -> tilgangsstyringRestClient.sjekkTilgang(
                identitetsnummer = identitetsnummer,
                authorizationHeader = authorizationHeader,
                beskrivelse = "slå opp saksnummer",
                correlationId = correlationId
            )
        }
    }


    post("/saksnummer") {
        val requestedIdentitetsnummer = call.receive<HentSaksnummerRequestBody>().identitetsnummer
        val identitetsnummer = setOf(requestedIdentitetsnummer)

        val jwtPrincipal = call.authentication.principal<JWTPrincipal>()
            ?: return@post call.respond(HttpStatusCode.Unauthorized)

        val authorizationHeader = call.request.header(HttpHeaders.Authorization)
            ?: return@post call.respond(HttpStatusCode.Unauthorized)

        val harTilgangTilSaksnummer = harTilgangTilSaksnummer(
            authorizationHeader = authorizationHeader,
            jwtPrincipal = jwtPrincipal,
            correlationId = call.correlationId(),
            identitetsnummer = identitetsnummer
        )

        if (!harTilgangTilSaksnummer) {
            return@post call.respond(HttpStatusCode.Forbidden)
        }

        val saksnummer = saksnummerRepository.hentSaksnummer(identitetsnummer) ?: saksnummerRepository.hentSaksnummer(
            hentIdentPdlMediator.hentIdentitetsnummer(identitetsnummer, call.correlationId()).getOrDefault(requestedIdentitetsnummer, emptySet())
        )

        if (saksnummer != null)
            call.respond(HentSaksnummerResponseBody(saksnummer))
        else
            call.respond(HttpStatusCode.NotFound)
    }
}
