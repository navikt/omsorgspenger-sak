package no.nav.omsorgspenger.sak

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.opentable.db.postgres.embedded.FlywayPreparer
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
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
    private lateinit var flyway: Flyway
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
    fun `Accepterar gyldigt behov`() {
        val id = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
        val (behovssekvensId, behovssekvens) = behovssekvens(
                behov = Behov,
                id = id,
                identitetsnummer = "11111111111")

        assertEquals(id, behovssekvensId)

        rapid.sendTestMessage(behovssekvens)

        assertEquals(1, rapid.inspektør.size)

    }

    @Test
    fun `Tar ikke emot ugyldigt behov`() {
        val id = "01BX5ZZKBKACTAV9WEVGEMMVS0"
        val (behovssekvensId, behovssekvens) = behovssekvens(
                behov = "IkkeGyldig",
                id = id,
                identitetsnummer = "111111111112")

        rapid.sendTestMessage(behovssekvens)

        assertEquals(0, rapid.inspektør.size)
    }

    internal companion object {
        const val Behov = "HentOmsorgspengerSaksnummer"

        private fun behovssekvens(
                behov: String,
                id: String,
                identitetsnummer: String
        ) = Behovssekvens(
                id = id,
                correlationId = UUID.randomUUID().toString(),
                behov = arrayOf(Behov(behov,
                        mapOf(
                                "identitetsnummer" to identitetsnummer
                        )))
        ).keyValue
    }

}