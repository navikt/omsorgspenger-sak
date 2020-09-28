package no.nav.omsorgspenger.sak

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HentOmsorgspengerSaksnummerTest {

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private var rapid = TestRapid()

    @BeforeAll
    internal fun setupAll(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
                .setOverrideWorkingDirectory(postgresPath.toFile())
                .setDataDirectory(postgresPath.resolve("datadir"))
                .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection

        dataSource = HikariDataSource(HikariConfig().apply {
            this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        })

        rapid.apply {
            HentOmsorgspengerSaksnummer(this, dataSource)
        }

    }

    @AfterAll
    internal fun afterAll() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @BeforeEach
    fun reset() {
        Flyway
                .configure()
                .dataSource(dataSource)
                .load()
                .also {
                    it.clean()
                    it.migrate()
                }
        rapid.reset()
    }

    @Test
    fun `Tar ikke emot ugyldigt behov`() {
        val (behovssekvensId, behovssekvens) = nyBehovsSekvens(
                behov = "IkkeGyldig",
                identitetsnummer = "111111111112")

        rapid.sendTestMessage(behovssekvens)

        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `Hæmtar existerande saksnummer`() {

        val (behovssekvensId, behovssekvens) = nyBehovsSekvens(
                behov = BEHOV,
                identitetsnummer = "11111111111")

        rapid.sendTestMessage(behovssekvens)

        val saksnummerIDatabas = "TEST12345"
        val saksnummerFraRiver = rapid.inspektør.message(0).at(LØSNINGSJSONPOINTER).asText()

        assertEquals(saksnummerIDatabas, saksnummerFraRiver)
    }

    @Test
    fun `Får samma resultat ifall samma fnr sænds två gånger`() {

        val (behovssekvensId1, behovssekvens1) = nyBehovsSekvens(
                behov = BEHOV,
                identitetsnummer = "11111111113")

        rapid.sendTestMessage(behovssekvens1)

        val (behovssekvensId2, behovssekvens2) = nyBehovsSekvens(
                behov = BEHOV,
                identitetsnummer = "11111111113")
        rapid.sendTestMessage(behovssekvens2)

        val løsningsSaksnummer1 = rapid.inspektør.message(0).at(LØSNINGSJSONPOINTER).asText()
        val løsningsSaksnummer2 = rapid.inspektør.message(1).at(LØSNINGSJSONPOINTER).asText()

        assertEquals(løsningsSaksnummer1, løsningsSaksnummer2)
    }

    internal companion object {
        const val BEHOV = "HentOmsorgspengerSaksnummer"
        const val LØSNINGSJSONPOINTER = "/@løsninger/HentOmsorgspengerSaksnummer/saksnummer"

        private fun nyBehovsSekvens(
                behov: String,
                identitetsnummer: String
        ) = Behovssekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                correlationId = UUID.randomUUID().toString(),
                behov = arrayOf(Behov(behov,
                        mapOf(
                                "identitetsnummer" to identitetsnummer
                        )))
        ).keyValue
    }

}