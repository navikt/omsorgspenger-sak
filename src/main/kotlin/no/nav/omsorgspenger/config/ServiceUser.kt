package no.nav.omsorgspenger.config

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private const val secretBase = "/var/run/secrets/nais.io/service_user"
private val secretBasePath: Path = Paths.get(secretBase)

fun readServiceUserCredentials() = ServiceUser(
    username = Files.readString(secretBasePath.resolve("username")),
    password = Files.readString(secretBasePath.resolve("password"))
)

data class ServiceUser(
    val username: String,
    val password: String
)