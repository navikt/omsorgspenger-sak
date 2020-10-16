package no.nav.omsorgspenger.testutils

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.*
import no.nav.omsorgspenger.ApplicationContext
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

internal class ApplicationContextExtension : ParameterResolver {

    @KtorExperimentalAPI
    companion object {

        val tempDir = createTempDir("tmp_postgres")

        private val embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(tempDir)
            .setDataDirectory(tempDir.resolve("datadir"))
            .start()

        private val applicationContextBuilder = ApplicationContext.Builder(
            env = mapOf(
                "DATABASE_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
            )
        )

        private val applicationContext = applicationContextBuilder.build()

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                embeddedPostgres.postgresDatabase.connection.close()
                embeddedPostgres.close()
            })
        }

        private val støttedeParametre = listOf(
            ApplicationContext.Builder::class.java,
            ApplicationContext::class.java,
            EmbeddedPostgres::class.java
        )
    }

    @KtorExperimentalAPI
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    @KtorExperimentalAPI
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return when (parameterContext.parameter.type) {
            ApplicationContext::class.java -> applicationContext
            EmbeddedPostgres::class.java -> embeddedPostgres
            else -> applicationContextBuilder
        }
    }
}