package no.nav.omsorgspenger.sak

import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class HentIdentPdlMediatorTest(
    applicationContext: ApplicationContext
) {

    private val hentIdentPdlMediator = applicationContext.hentIdentPdlMediator

    @Test
    fun `Flera ident i input ger riktigt svar`() {
        val response = runBlocking {
            hentIdentPdlMediator.hentIdentitetsnummer(setOf("12345678910", "12345678911"), CorrelationId.generate())
        }
        assert(response.values.contains(setOf("12345678910", "9987654321")))
    }

    @Test
    fun `Svar fra PDL uten innehåll ger tomt svar`() {
        val response = runBlocking {
            hentIdentPdlMediator.hentIdentitetsnummer(setOf("404"), CorrelationId.generate())
        }
        assert(response.isEmpty())
    }

    @Test
    fun `Inget svar från PDL failar`() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                hentIdentPdlMediator.hentIdentitetsnummer(setOf("500"), CorrelationId.generate())
            }
        }
    }

}