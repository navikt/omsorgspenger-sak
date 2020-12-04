package no.nav.omsorgspenger.apis

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.omsorgspengerSak
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class HealthApiTest(
    private val applicationContext: ApplicationContext) {

    @Test
    fun `Test health end point`() {
        withTestApplication({
            omsorgspengerSak(applicationContext)
        }) {
            handleRequest(HttpMethod.Get, "/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
            }
        }
    }
}