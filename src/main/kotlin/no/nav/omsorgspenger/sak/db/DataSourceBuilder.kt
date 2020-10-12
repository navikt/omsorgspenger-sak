package no.nav.omsorgspenger.sak.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

internal class DataSourceBuilder(env: Map<String, String>) {

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DATABASE_URL"] ?: String.format(
                "jdbc:postgresql://%s:%s/%s%s",
                requireNotNull(env["DATABASE_HOST"]) { "database host must be set if jdbc url is not provided" },
                requireNotNull(env["DATABASE_PORT"]) { "database port must be set if jdbc url is not provided" },
                requireNotNull(env["DATABASE_DATABASE"]) { "database name must be set if jdbc url is not provided" },
                env["DATABASE_USERNAME"]?.let { "?user=$it" } ?: "")

        env["DATABASE_USERNAME"]?.let { this.username = it }
        env["DATABASE_PASSWORD"]?.let { this.password = it }

        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
        //dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
    }

    init {
        checkNotNull(env["DATABASE_DATABASE"]) { "database must be set" }
    }

    fun getDataSource(): DataSource {
        return HikariDataSource(hikariConfig)
    }

    fun migrate() {
        Flyway.configure()
                .dataSource(getDataSource())
                .load()
                .migrate()
    }

}