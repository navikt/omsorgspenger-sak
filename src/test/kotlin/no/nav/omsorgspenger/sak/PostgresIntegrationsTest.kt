package no.nav.omsorgspenger.sak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension.Companion.embeddedPostgress
import no.nav.omsorgspenger.testutils.ApplicationContextExtension.Companion.testApplicationContextBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import no.nav.omsorgspenger.testutils.wiremock.pdlIdentIngenHistorikk_1

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PostgresIntegrationsTest {
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var rapid : TestRapid

    @BeforeAll
    internal fun setupAll(@TempDir tempDirPath: Path) {
        embeddedPostgres = embeddedPostgress(tempDirPath.toFile())
        rapid = TestRapid().also {
            it.registerApplicationContext(testApplicationContextBuilder(embeddedPostgres).build())
        }
    }

    @Test
    fun `Sænder inte løsning ifall postgres ær nere`() {
        val (_, behovssekvens) = nyBehovsSekvens(
                behov = HentOmsorgspengerSaksnummerTest.BEHOV,
                identitetsnummer = setOf(pdlIdentIngenHistorikk_1))

        embeddedPostgres.close()

        rapid.sendTestMessage(behovssekvens)

        assertEquals(0, rapid.inspektør.size)
    }

    internal companion object {
        private fun nyBehovsSekvens(
                behov: String,
                identitetsnummer: Set<String>
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
