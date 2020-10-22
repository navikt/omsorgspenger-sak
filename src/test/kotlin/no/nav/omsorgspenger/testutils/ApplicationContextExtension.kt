package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl
import no.nav.omsorgspenger.ApplicationContext
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.omsorgspenger.client.pdl.PdlClient
import no.nav.omsorgspenger.config.ServiceUser
import no.nav.omsorgspenger.testutils.wiremock.pdlApiBaseUrl
import no.nav.omsorgspenger.testutils.wiremock.stubPdlApi

internal class ApplicationContextExtension : ParameterResolver {

    internal companion object {

        internal fun embeddedPostgress(tempDir: File) = EmbeddedPostgres.builder()
                .setOverrideWorkingDirectory(tempDir)
                .setDataDirectory(tempDir.resolve("datadir"))
                .start()

        internal fun testApplicationContextBuilder(
                embeddedPostgres: EmbeddedPostgres,
                wireMockServer: WireMockServer? = null
        ) = ApplicationContext.Builder(
                env = mapOf(
                        "DATABASE_HOST" to "localhost",
                        "DATABASE_PORT" to "${embeddedPostgres.port}",
                        "DATABASE_DATABASE" to "postgres",
                        "DATABASE_USERNAME" to "postgres",
                        "DATABASE_PASSWORD" to "postgres",
                        "PDL_BASE_URL" to Companion.wireMockServer.pdlApiBaseUrl(),
                        "PDL_API_GW_KEY" to "testApiKeyJoark",
                        "STS_TOKEN_ENDPOINT" to Companion.wireMockServer.getNaisStsTokenUrl(),
                        "STS_API_GW_KEY" to "testApiKeySts"
                ).let {
                    if (wireMockServer != null) {
                        it.plus(
                                mapOf(
                                        "AZURE_V2_ISSUER" to Azure.V2_0.getIssuer(),
                                        "AZURE_V2_JWKS_URI" to (wireMockServer.getAzureV2JwksUrl()),
                                        "AZURE_APP_CLIENT_ID" to "omsorgspenger-sak"
                                )
                        )
                    } else it
                },
                serviceUser = ServiceUser("foo", "bar")
        )

        private val wireMockServer = WireMockBuilder()
                .withAzureSupport()
                .withNaisStsSupport()
                .build()
                .stubPdlApi()
        private val embeddedPostgres = embeddedPostgress(createTempDir("tmp_postgres"))
        private val applicationContext = testApplicationContextBuilder(embeddedPostgres, wireMockServer).build()

        init {
            Runtime.getRuntime().addShutdownHook(
                    Thread {
                        embeddedPostgres.postgresDatabase.connection.close()
                        embeddedPostgres.close()
                        wireMockServer.stop()
                    }
            )
        }

        private val støttedeParametre = listOf(
                ApplicationContext::class.java
        )
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return applicationContext
    }
}
