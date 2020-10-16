package no.nav.omsorgspenger.sak

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class PostgresIntegrationsTest(
    private val applicationContext: ApplicationContext,
    private val embeddedPostgres: EmbeddedPostgres) {
    private var rapid = TestRapid().also {
        it.registerApplicationContext(applicationContext)
    }

    @Test
    fun `Sænder inte løsning ifall postgres ær nere`() {
        val (_, behovssekvens) = nyBehovsSekvens(
                behov = HentOmsorgspengerSaksnummerTest.BEHOV,
                identitetsnummer = "11111111115")

        embeddedPostgres.close()

        rapid.sendTestMessage(behovssekvens)

        assertEquals(0, rapid.inspektør.size)
    }

    internal companion object {
        private fun nyBehovsSekvens(
                behov: String,
                identitetsnummer: String
        ) = Behovssekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
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