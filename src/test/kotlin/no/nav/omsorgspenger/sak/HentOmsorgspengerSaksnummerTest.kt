package no.nav.omsorgspenger.sak

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import no.nav.omsorgspenger.testutils.wiremock.PdlEnFinnesEnFinnesIkke
import no.nav.omsorgspenger.testutils.wiremock.pdlIdentIngenHistorikk_1
import no.nav.omsorgspenger.testutils.wiremock.pdlIdentMedHistorikk
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
                identitetsnummer = setOf("111111111112"))

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
        val saksnummerFraRiver = rapid.inspektør.message(0).at(løsningsJsonPointer(identitetsnummer)).asText()

        assertEquals(saksnummerIDatabas, saksnummerFraRiver)
    }

    @Test
    fun `Får samma resultat ifall samma fnr sænds två gånger`() {

        val identitetsnummer = "11111111113"

        val (_, behovssekvens1) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                behov = BEHOV,
                identitetsnummer = setOf(identitetsnummer)
        )

        rapid.sendTestMessage(behovssekvens1)

        val (_, behovssekvens2) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                behov = BEHOV,
                identitetsnummer = setOf(identitetsnummer)
        )
        rapid.sendTestMessage(behovssekvens2)

        assertEquals(2, rapid.inspektør.size)
        val løsningsSaksnummer1 = rapid.inspektør.message(0).at(løsningsJsonPointer(identitetsnummer)).asText()
        val løsningsSaksnummer2 = rapid.inspektør.message(1).at(løsningsJsonPointer(identitetsnummer)).asText()

        assertEquals(løsningsSaksnummer1, løsningsSaksnummer2)
    }

    @Test
    fun `Samma saksnummer ifall ett FNR sænder två behov med olika ID`() {
        val identitetsnummer = "01111111115"
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
        val løsningsSaksnummer1 = rapid.inspektør.message(0).at(løsningsJsonPointer(identitetsnummer)).asText()
        val løsningsSaksnummer2 = rapid.inspektør.message(1).at(løsningsJsonPointer(identitetsnummer)).asText()

        assertEquals(løsningsSaksnummer1, løsningsSaksnummer2)
    }

    @Test
    fun `Hente saksnummer for fler personer i samme behov`() {

        val (_, behovsSekvens) = nyBehovsSekvens(
            id = "01EKW89QKK5YZ0XW2QQYS0TB8D",
            behov = BEHOV,
            identitetsnummer = setOf(
                pdlIdentIngenHistorikk_1,
                "11111111112"
            )
        )

        rapid.sendTestMessage(behovsSekvens)

        assertEquals(1, rapid.inspektør.size)
        val løsningsSaksnummer1 = rapid.inspektør.message(0).at(løsningsJsonPointer("11111111111")).asText()
        assertEquals("TEST12345", løsningsSaksnummer1)
        val løsningsSaksnummer2 = rapid.inspektør.message(0).at(løsningsJsonPointer("11111111112")).asText()
        assertEquals("TEST67891", løsningsSaksnummer2)
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

        val løsningsSaksnummer = rapid.inspektør.message(0).at(løsningsJsonPointer(pdlIdentMedHistorikk.gjeldende)).asText()
        assertEquals("SAK1", løsningsSaksnummer)
    }

    @Test
    internal fun `Oppretter ikke sak på personer som ikke finnes`() {
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

        rapid.sendTestMessage(behovssekvens)

        val saksnummerExpectedFinnes = applicationContext.saksnummerRepository.hentSaksnummer(finnes.historiske.toSet())
        val saksnummerExpectedIkkeFinnes = applicationContext.saksnummerRepository.hentSaksnummer(setOf(finnesIkke))

        assertNotNull(saksnummerExpectedFinnes)
        assertNull(saksnummerExpectedIkkeFinnes)
    }

    internal companion object {
        const val BEHOV = "HentOmsorgspengerSaksnummer"
        fun løsningsJsonPointer(identitetsnummer: String) = "/@løsninger/HentOmsorgspengerSaksnummer/saksnummer/$identitetsnummer"

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
