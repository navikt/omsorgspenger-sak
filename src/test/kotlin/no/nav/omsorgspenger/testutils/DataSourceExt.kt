package no.nav.omsorgspenger.testutils

import org.flywaydb.core.Flyway
import javax.sql.DataSource

internal fun DataSource.cleanAndMigrate() {
    Flyway
        .configure()
        .cleanDisabled(false)
        .dataSource(this)
        .load()
        .also {
            it.clean()
            it.migrate()
        }
}