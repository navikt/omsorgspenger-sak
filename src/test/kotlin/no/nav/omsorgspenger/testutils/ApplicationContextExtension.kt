package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl
import no.nav.omsorgspenger.ApplicationContext
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import java.net.URI
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.omsorgspenger.config.ServiceUser
import no.nav.omsorgspenger.testutils.wiremock.pdlApiBaseUrl
import no.nav.omsorgspenger.testutils.wiremock.stubPdlApi
import no.nav.omsorgspenger.testutils.wiremock.stubTilgangApi
import no.nav.omsorgspenger.testutils.wiremock.tilgangApiBaseUrl

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
                        "STS_TOKEN_ENDPOINT" to Companion.wireMockServer.getNaisStsTokenUrl(),
                        "TILGANGSSTYRING_URL" to Companion.wireMockServer.tilgangApiBaseUrl(),
                        "PROXY_SCOPES" to "test/.default"
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
                serviceUser = ServiceUser("foo", "bar"),
                accessTokenClient = ClientSecretAccessTokenClient(
                        clientId = "omsorgspenger-sak",
                        clientSecret = "azureSecret",
                        tokenEndpoint = URI(Companion.wireMockServer.getAzureV2TokenUrl())
                )
        )

        private val wireMockServer = WireMockBuilder()
                .withAzureSupport()
                .build()
                .stubPdlApi()
                .stubTilgangApi()
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
