package no.nav.omsorgspenger

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

internal class DataSourceBuilder(env: Map<String, String>) {
    private val databaseName = env["DATABASE_DATABASE"]

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DATABASE_URL"]
        username = env["DATABASE_USERNAME"]
        password = "debug" // env["DATABASE_PASSWORD"]
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
    }

    init {
        checkNotNull(env["DATABASE_DATABASE"]) { "database must be set" }
        checkNotNull(env["DATABASE_URL"]) { "jdbcUrl must be set" }
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