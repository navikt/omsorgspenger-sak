package no.nav.omsorgspenger.config

internal typealias Environment = Map<String, String>

internal fun Environment.hentRequiredEnv(key: String) : String = requireNotNull(get(key)) {
    "Environment variable $key må være satt"
}