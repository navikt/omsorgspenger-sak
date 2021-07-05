package no.nav.omsorgspenger.apis

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
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
    fun `Henter saksnummer for en personident`() {
        withTestApplication({
            omsorgspengerSak(applicationContext)
        }) {
            handleRequest(HttpMethod.Post, "/saksnummer") {
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer ${gyldigToken()}")
                setBody(
                    """
                {
                    "identitetsnummer": "$pdlIdentIngenHistorikk_1"
                }
                    """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())

                @Language("JSON")
                val forventetResponse = """
                    {
                      "saksnummer": "TEST12345"
                    }
                """.trimIndent()

                JSONAssert.assertEquals(forventetResponse, response.content, true)
            }
        }
    }

    @Test
    fun `Gir 404 for person uten tilhørende sak`() {
        withTestApplication({
            omsorgspengerSak(applicationContext)
        }) {
            handleRequest(HttpMethod.Post, "/saksnummer") {
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer ${gyldigToken()}")
                addHeader("X-Correlation-Id", "${CorrelationId.generate()}")
                setBody(
                    """
                {
                    "identitetsnummer": "98798798787"
                }
                    """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @Test
    fun `Gir 403 dersom ingen tilgang`() {
        withTestApplication({
            omsorgspengerSak(applicationContext)
        }) {
            handleRequest(HttpMethod.Post, "/saksnummer") {
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer ${gyldigToken()}")
                addHeader("Nav-Call-Id", "CallId_1615826230671_791407618")
                setBody(
                    """
                {
                    "identitetsnummer": "$personident403"
                }
                    """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test
    fun `Systembruker får hentet saksnummer tross skjermet person`() {
        withTestApplication({
            omsorgspengerSak(applicationContext)
        }) {
            handleRequest(HttpMethod.Post, "/saksnummer") {
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer ${gyldigToken(accessAsApplication = true)}")
                addHeader("X-Correlation-Id", "CallId_1615826230671_791407619")
                setBody(
                    """
                    {
                        "identitetsnummer": "$personident403"
                    }
                    """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())

                @Language("JSON")
                val forventetResponse = """
                    {
                      "saksnummer": "SAK2"
                    }
                """.trimIndent()

                JSONAssert.assertEquals(forventetResponse, response.content, true)
            }
        }
    }
}

internal fun gyldigToken(accessAsApplication: Boolean = false) = Azure.V2_0.generateJwt(
    clientId = "any",
    audience = "omsorgspenger-sak",
    clientAuthenticationMode = Azure.ClientAuthenticationMode.CLIENT_SECRET,
    accessAsApplication = accessAsApplication
)
