package no.nav.omsorgspenger.config

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Base64

const val secretBase = "/var/run/secrets/nais.io/service_user"
val secretBasePath: Path = Paths.get(secretBase)

fun readServiceUserCredentials() = ServiceUser(
        username = Files.readString(secretBasePath.resolve("username")),
        password = Files.readString(secretBasePath.resolve("password"))
)

data class ServiceUser(
        val username: String,
        val password: String
) {
    val basicAuth = "Basic ${Base64.getEncoder().encodeToString("$username:$password".toByteArray(Charsets.UTF_8))}"
}