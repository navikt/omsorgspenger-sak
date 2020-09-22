package no.nav.omsorgspenger

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

internal class DataSourceBuilder(env: Map<String, String>) {
    private val databaseName = env["DATABASE_DATABASE"]

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DATABASE_URL"] ?: String.format(
                "jdbc:postgresql://%s:%s/%s%s",
                requireNotNull(env["DATABASE_HOST"]) { "database host must be set if jdbc url is not provided" },
                requireNotNull(env["DATABASE_PORT"]) { "database port must be set if jdbc url is not provided" },
                requireNotNull(databaseName) { "database name must be set if jdbc url is not provided" },
                env["DATABASE_USERNAME"]?.let { "?user=$it" } ?: "")

        env["DATABASE_USERNAME"]?.let { this.username = it }
        env["DATABASE_PASSWORD"]?.let { this.password = it }
        this.password = "debug"

        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
    }

    init {

        checkNotNull(env["DATABASE_DATABASE"]) { "database must be set" }
    }

    fun getDataSource(role: Role = Role.User): DataSource {
        return HikariDataSource(hikariConfig)
    }

    fun migrate() {
        var initSql: String? = null
        initSql = "SET ROLE \"$databaseName-${Role.Admin}\""

        runMigration(getDataSource(Role.Admin), initSql)
    }

    private fun runMigration(dataSource: DataSource, initSql: String? = null) =
            Flyway.configure()
                    .dataSource(dataSource)
                    .initSql(initSql)
                    .load()
                    .migrate()

    enum class Role {
        Admin, User, ReadOnly;

        override fun toString() = name.toLowerCase()
    }
}