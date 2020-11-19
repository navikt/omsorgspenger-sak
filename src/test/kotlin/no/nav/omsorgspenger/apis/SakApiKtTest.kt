package no.nav.omsorgspenger.apis

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.omsorgspengerSak
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import no.nav.omsorgspenger.testutils.wiremock.pdlIdentIngenHistorikk_1
import no.nav.omsorgspenger.testutils.wiremock.personident403
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertEquals

@ExtendWith(ApplicationContextExtension::class)
internal class SakApiKtTest(private val applicationContext: ApplicationContext) {

    @BeforeEach
    fun reset() {
        applicationContext.dataSource.cleanAndMigrate()
    }

    @Test
    internal fun `Henter saksnummer for en personident`() {
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
    internal fun `Gir 404 for person uten tilhørende sak`() {
        withTestApplication({
            omsorgspengerSak(applicationContext)
        }) {
            handleRequest(HttpMethod.Post, "/saksnummer") {
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer ${gyldigToken()}")
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
    internal fun `Gir 403 dersom ingen tilgang`() {
        withTestApplication({
            omsorgspengerSak(applicationContext)
        }) {
            handleRequest(HttpMethod.Post, "/saksnummer") {
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer ${gyldigToken()}")
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
}

internal fun gyldigToken() = Azure.V2_0.generateJwt(
    clientId = "any",
    audience = "omsorgspenger-sak",
    clientAuthenticationMode = Azure.ClientAuthenticationMode.CLIENT_SECRET,
)
