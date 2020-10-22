package no.nav.omsorgspenger.sak

import java.lang.IllegalStateException
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class HentIdentPdlMediatorTest(
        applicationContext: ApplicationContext) {

    private val hentIdentPdlMediator = applicationContext.hentIdentPdlMediator

    @Test
    fun `Flera ident i input ger riktigt svar`() {
        val response = runBlocking {
            hentIdentPdlMediator.hentIdenter(setOf("12345678910", "12345678911"))
        }
        assert(response.values.contains(setOf("12345678910", "9987654321")))
    }

    @Test
    fun `Svar fra PDL uten innehåll ger tomt svar`() {
            val response = runBlocking {
                hentIdentPdlMediator.hentIdenter(setOf("404"))
            }
        assert(response.isEmpty())
    }

    @Test
    fun `Inget svar från PDL kaster illegalstate`() {
        assertFailsWith(IllegalStateException::class) {
            val response = runBlocking {
                hentIdentPdlMediator.hentIdenter(setOf("500"))
            }
        }

    }

}