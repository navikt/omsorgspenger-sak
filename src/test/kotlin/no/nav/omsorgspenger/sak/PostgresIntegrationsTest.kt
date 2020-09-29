package no.nav.omsorgspenger.sak

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PostgresIntegrationsTest {

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

    @Test
    fun `Sænder inte løsning ifall postgres ær nere`() {
        val (behovssekvensId, behovssekvens) = nyBehovsSekvens(
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
                behov = arrayOf(Behov(behov,
                        mapOf(
                                "identitetsnummer" to identitetsnummer
                        )))
        ).keyValue
    }
}