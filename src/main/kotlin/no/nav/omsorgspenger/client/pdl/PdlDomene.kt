package no.nav.omsorgspenger.client.pdl

typealias Identitetsnummer = String

data class GraphQLQuery(
        val query: String,
        val variables: Variables
)

data class Variables(
        val identer: List<String>
)

data class PdlError(
        val message: String,
        val locations: List<PdlErrorLocation>,
        val path: List<String>?,
        val extensions: PdlErrorExtension
)

data class PdlErrorLocation(
        val line: Int?,
        val column: Int?
)

data class PdlErrorExtension(
        val code: String?,
        val classification: String
)

fun hentIdenterQuery(fnr: Set<String>): GraphQLQuery {
    val query = "query(\$ident: ID!) { hentIdenter(identer: \$identer, grupper: [FOLKEREGISTERIDENT], historikk: true) { ident, identer{ ident }, code } }"
    return GraphQLQuery(query, Variables(fnr.toList()))
}