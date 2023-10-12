package no.nav.omsorgspenger.apis

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.omsorgspengerSak
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import no.nav.omsorgspenger.testutils.wiremock.pdlIdentIngenHistorikk_1
import no.nav.omsorgspenger.testutils.wiremock.personident403
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert

@ExtendWith(ApplicationContextExtension::class)
internal class SakApiKtTest(private val applicationContext: ApplicationContext) {

    @BeforeEach
    fun reset() {
        applicationContext.dataSource.cleanAndMigrate()
    }

    @Test
    fun `Henter saksnummer for en personident`() = testApplication {
        application {
            omsorgspengerSak(applicationContext)
        }

        client.post("/saksnummer") {
            header(HttpHeaders.Authorization, "Bearer ${gyldigToken()}")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""
                { "identitetsnummer": "$pdlIdentIngenHistorikk_1" }
            """.trimIndent())
        }.apply {
            assertEquals(HttpStatusCode.OK, this.status)

            @Language("JSON")
            val forventetResponse = """
                    {
                      "saksnummer": "TEST12345"
                    }
                """.trimIndent()

            JSONAssert.assertEquals(forventetResponse, this.bodyAsText(), true)
        }
    }

    @Test
    fun `Gir 404 for person uten tilhørende sak`() = testApplication {
        application {
            omsorgspengerSak(applicationContext)
        }

        client.post("/saksnummer") {
            header(HttpHeaders.Authorization, "Bearer ${gyldigToken()}")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header("X-Correlation-Id", "${CorrelationId.generate()}")
            setBody("""
                { "identitetsnummer": "404" }
            """.trimIndent())
        }.apply {
            assertEquals(HttpStatusCode.NotFound, this.status)
        }
    }

    @Test
    fun `Gir 403 dersom ingen tilgang`() = testApplication {
        application {
            omsorgspengerSak(applicationContext)
        }

        client.post("/saksnummer") {
            header(HttpHeaders.Authorization, "Bearer ${gyldigToken()}")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header("Nav-Call-Id", "CallId_1615826230671_791407618")
            setBody("""
                { "identitetsnummer": "$personident403" }
            """.trimIndent())
        }.apply {
            assertEquals(HttpStatusCode.Forbidden, this.status)
        }
    }

    @Test
    fun `Systembruker får hentet saksnummer tross skjermet person`() = testApplication {
        application {
            omsorgspengerSak(applicationContext)
        }

        client.post("/saksnummer") {
            header(HttpHeaders.Authorization, "Bearer ${gyldigToken(accessAsApplication = true)}")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header("X-Correlation-Id", "CallId_1615826230671_791407619")
            setBody("""
                { "identitetsnummer": "$personident403" }
            """.trimIndent())
        }.apply {
            assertEquals(HttpStatusCode.OK, this.status)

            @Language("JSON")
            val forventetResponse = """
                    {
                      "saksnummer": "SAK2"
                    }
                """.trimIndent()

            JSONAssert.assertEquals(forventetResponse, this.bodyAsText(), true)
        }
    }
}

internal fun gyldigToken(accessAsApplication: Boolean = false) = Azure.V2_0.generateJwt(
    clientId = "any",
    audience = "omsorgspenger-sak",
    clientAuthenticationMode = Azure.ClientAuthenticationMode.CLIENT_SECRET,
    accessAsApplication = accessAsApplication
)
