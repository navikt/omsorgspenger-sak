package no.nav.omsorgspenger.sak

import com.fasterxml.jackson.databind.JsonNode
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import no.nav.omsorgspenger.testutils.wiremock.*
import no.nav.omsorgspenger.testutils.wiremock.PdlEnFinnesEnFinnesIkke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertTrue

@ExtendWith(ApplicationContextExtension::class)
internal class HentOmsorgspengerSaksnummerTest(
    private val applicationContext: ApplicationContext) {
    private var rapid = TestRapid().also {
        it.registerApplicationContext(applicationContext)
    }

    @BeforeEach
    fun reset() {
        applicationContext.dataSource.cleanAndMigrate()
        rapid.reset()
    }

    @Test
    fun `Tar ikke emot ugyldigt behov`() {
        val (_, behovssekvens) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                behov = "IkkeGyldig",
                identitetsnummer = setOf(pdlIdentIngenHistorikk_2))

        rapid.sendTestMessage(behovssekvens)

        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `Hæmtar existerande saksnummer`() {
        val identitetsnummer = pdlIdentIngenHistorikk_1

        val (_, behovssekvens) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                behov = BEHOV,
                identitetsnummer = setOf(identitetsnummer)
        )

        rapid.sendTestMessage(behovssekvens)

        val saksnummerIDatabas = "TEST12345"
        val saksnummerFraRiver = rapid.inspektør.message(0).at(løsningsJsonPointer(identitetsnummer)).asTextAssertNotBlank()

        assertEquals(saksnummerIDatabas, saksnummerFraRiver)
    }

    @Test
    fun `Samma saksnummer ifall ett FNR sænder två behov med olika ID`() {
        val identitetsnummer = pdlIdentIngenHistorikk_3
        val (_, behovsSekvens1) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                behov = BEHOV,
                identitetsnummer = setOf(identitetsnummer)
        )

        rapid.sendTestMessage(behovsSekvens1)

        val (_, behovsSekvens2) = nyBehovsSekvens(
                id = "01EKEVACZM1T55PY5XDEPR5B4P",
                behov = BEHOV,
                identitetsnummer = setOf(identitetsnummer)
        )

        rapid.sendTestMessage(behovsSekvens2)

        assertEquals(2, rapid.inspektør.size)
        val løsningsSaksnummer1 = rapid.inspektør.message(0).at(løsningsJsonPointer(identitetsnummer)).asTextAssertNotBlank()
        val løsningsSaksnummer2 = rapid.inspektør.message(1).at(løsningsJsonPointer(identitetsnummer)).asTextAssertNotBlank()

        assertEquals(løsningsSaksnummer1, løsningsSaksnummer2)
    }

    @Test
    fun `Hente saksnummer for fler personer i samme behov`() {

        val (_, behovsSekvens) = nyBehovsSekvens(
            id = "01EKW89QKK5YZ0XW2QQYS0TB8D",
            behov = BEHOV,
            identitetsnummer = setOf(
                pdlIdentIngenHistorikk_1,
                pdlIdentIngenHistorikk_2
            )
        )

        rapid.sendTestMessage(behovsSekvens)

        assertEquals(1, rapid.inspektør.size)
        val løsningsSaksnummer1 = rapid.inspektør.message(0).at(løsningsJsonPointer(pdlIdentIngenHistorikk_1)).asTextAssertNotBlank()
        assertEquals("TEST12345", løsningsSaksnummer1)
        val løsningsSaksnummer2 = rapid.inspektør.message(0).at(løsningsJsonPointer(pdlIdentIngenHistorikk_2)).asTextAssertNotBlank()
        assertEquals("TEST67891", løsningsSaksnummer2)
    }

    @Test
    fun `Eksisterende saksnummer har ikke OP-prefix men nye får det`() {

        val (_, behovsSekvens) = nyBehovsSekvens(
            id = "01ESNXD9P6EQ02RRKBE24RV2JC",
            behov = BEHOV,
            identitetsnummer = setOf(
                pdlIdentIngenHistorikk_1,
                pdlIdentIngenHistorikk_3
            )
        )

        rapid.sendTestMessage(behovsSekvens)

        assertEquals(1, rapid.inspektør.size)
        val løsningsSaksnummer1 = rapid.inspektør.message(0).at(løsningsJsonPointer(pdlIdentIngenHistorikk_1)).asTextAssertNotBlank()
        assertEquals("TEST12345", løsningsSaksnummer1)
        val løsningsSaksnummer2 = rapid.inspektør.message(0).at(løsningsJsonPointer(pdlIdentIngenHistorikk_3)).asTextAssertNotBlank()
        assertTrue(løsningsSaksnummer2.startsWith("OP"))
        assertTrue(løsningsSaksnummer2.length == 7)
    }

    @Test
    fun `Hente sak fra historisk ident`() {
        val (_, behovsSekvens) = nyBehovsSekvens(
                id = "01EKW89QKK5YZ0XW2QQYS0TB8D",
                behov = BEHOV,
                identitetsnummer = setOf(
                        pdlIdentMedHistorikk.gjeldende
                )
        )

        rapid.sendTestMessage(behovsSekvens)

        val løsningsSaksnummer = rapid.inspektør.message(0).at(løsningsJsonPointer(pdlIdentMedHistorikk.gjeldende)).asTextAssertNotBlank()
        assertEquals("SAK1", løsningsSaksnummer)
    }

    @Test
    fun `Feiler om man forsøker å opprette saksnummer på person som ikke finnes`() {
        val finnes = PdlEnFinnesEnFinnesIkke.finnes
        val finnesIkke = PdlEnFinnesEnFinnesIkke.finnesIkke
        val (_, behovssekvens) = nyBehovsSekvens(
            id = "01EKW89QKK5YZ0XW2QQYS0TB8D",
            behov = BEHOV,
            identitetsnummer = setOf(
                finnes.gjeldende,
                finnesIkke
            )
        )

        val sizeFør = rapid.inspektør.size
        rapid.sendTestMessage(behovssekvens)
        assertEquals(sizeFør, rapid.inspektør.size)
    }

    internal companion object {
        const val BEHOV = "HentOmsorgspengerSaksnummer"

        private fun JsonNode.asTextAssertNotBlank() = asText().also { assertFalse(it.isNullOrBlank()) }
        private fun løsningsJsonPointer(identitetsnummer: String) = "/@løsninger/HentOmsorgspengerSaksnummer/saksnummer/$identitetsnummer"

        private fun nyBehovsSekvens(
                id: String,
                behov: String,
                identitetsnummer: Set<String>
        ) = Behovssekvens(
                id = id,
                correlationId = UUID.randomUUID().toString(),
                behov = arrayOf(
                    Behov(
                        navn = behov,
                        input = mapOf(
                            "identitetsnummer" to identitetsnummer
                        )
                    )
                )
        ).keyValue
    }
}
