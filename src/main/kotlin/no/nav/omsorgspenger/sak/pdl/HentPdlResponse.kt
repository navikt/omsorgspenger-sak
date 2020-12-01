package no.nav.omsorgspenger.sak.pdl

data class HentPdlBolkResponse(val data: HentIdenterBolkInfo, val errors: List<PdlError>?)

data class HentIdenterBolkInfo(val hentIdenterBolk: List<IdenterBolk>?)

data class IdenterBolk(
        val ident: String,
        val identer: List<Ident>?,
        val code: String
)

data class Ident(
        val ident: String
)