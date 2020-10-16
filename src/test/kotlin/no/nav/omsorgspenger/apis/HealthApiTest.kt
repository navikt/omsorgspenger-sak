package no.nav.omsorgspenger.apis

import io.ktor.http.*
import no.nav.omsorgspenger.ApplicationContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/*
@ExtendWith(ApplicationContextExtension::class)
internal class HealthApiTest(
    private val applicationContext: ApplicationContext) {

    @Test
    fun `Test health end point`() {
        withTestApplication({
            omsorgspengerJournalføring(applicationContext)
        }) {
            handleRequest(HttpMethod.Get, "/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
            }
        }
    }
}
 */