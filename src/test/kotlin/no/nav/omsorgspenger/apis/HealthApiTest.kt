package no.nav.omsorgspenger.apis

import io.ktor.http.*
import io.ktor.server.testing.*
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