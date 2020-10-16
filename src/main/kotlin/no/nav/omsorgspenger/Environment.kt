package no.nav.omsorgspenger

internal typealias Environment = Map<String, String>

internal fun Environment.hentOptionalEnv(key: String) : String? = get(key)
internal fun Environment.hentRequiredEnv(key: String) : String = requireNotNull(get(key)) {
    "Environment variable $key må være satt"
}