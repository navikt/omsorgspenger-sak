package no.nav.omsorgspenger.sak

import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class HentIdentPdlMediatorTest(
        applicationContext: ApplicationContext) {

    private val hentIdentPdlMediator = applicationContext.hentIdentPdlMediator

    @Test
    fun `Flera ident i input`() {
        val response = runBlocking {
            hentIdentPdlMediator.hentIdenter(setOf("12345678910", "12345678911"))
        }
        assert(response.size == 2)
    }

    @Test
    fun `Svar fra PDL uten innehåll ger tomt svar`() {
        val response = runBlocking {
            hentIdentPdlMediator.hentIdenter(setOf("404"))
        }
        val result = response["404"]?.isEmpty() ?: false
        assert(result)
    }

}