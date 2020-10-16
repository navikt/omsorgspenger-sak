package no.nav.omsorgspenger.sak.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.omsorgspenger.Environment
import no.nav.omsorgspenger.hentOptionalEnv
import no.nav.omsorgspenger.hentRequiredEnv
import org.flywaydb.core.Flyway
import javax.sql.DataSource

internal class DataSourceBuilder(env: Environment) {

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env.hentOptionalEnv("DATABASE_URL") ?: String.format(
                "jdbc:postgresql://%s:%s/%s%s",
                env.hentRequiredEnv("DATABASE_HOST"),
                env.hentRequiredEnv("DATABASE_PORT"),
                env.hentRequiredEnv("DATABASE_DATABASE"),
                env.hentOptionalEnv("DATABASE_USERNAME")?.let { "?user=$it" } ?: "")

        env.hentOptionalEnv("DATABASE_USERNAME")?.let { this.username = it }
        env.hentOptionalEnv("DATABASE_PASSWORD")?.let { this.password = it }

        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
        //dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
    }

    internal fun build(): DataSource {
        return HikariDataSource(hikariConfig)
    }
}

internal fun DataSource.migrate() {
    Flyway.configure()
        .dataSource(this)
        .load()
        .migrate()
}