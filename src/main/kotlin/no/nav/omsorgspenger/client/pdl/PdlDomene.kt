package no.nav.omsorgspenger.client.pdl

typealias Identitetsnummer = String

data class GraphqlQuery(
        val query: String,
        val variables: Variables
)

data class PersonInfoGraphqlQuery(
        val query: String,
        val variables: Variables
)

data class Variables(
        val ident: List<String>
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

fun hentIdenterQuery(fnr: Set<String>): PersonInfoGraphqlQuery {
    val query = GraphqlQuery::class.java.getResource("/pdl/hentIdenterBolk.graphql").readText().replace("[\n\r]", "")
    return PersonInfoGraphqlQuery(query, Variables(fnr.toList()))
}