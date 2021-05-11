package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.omsorgspenger.ApplicationContext
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import no.nav.omsorgspenger.testutils.wiremock.pdlApiBaseUrl
import no.nav.omsorgspenger.testutils.wiremock.stubPdlApi
import no.nav.omsorgspenger.testutils.wiremock.stubTilgangApi
import no.nav.omsorgspenger.testutils.wiremock.tilgangApiBaseUrl
import java.nio.file.Files.createTempDirectory

internal class ApplicationContextExtension : ParameterResolver {

    internal companion object {
        private val wireMockServer = WireMockBuilder()
            .withAzureSupport()
            .build()
            .stubPdlApi()
            .stubTilgangApi()

        private val embeddedPostgres = embeddedPostgress(createTempDirectory("tmp_postgres").toFile())
        private val applicationContext = testApplicationContextBuilder(embeddedPostgres, wireMockServer).build()

        internal fun embeddedPostgress(tempDir: File) = EmbeddedPostgres.builder()
                .setOverrideWorkingDirectory(tempDir)
                .setDataDirectory(tempDir.resolve("datadir"))
                .start()

        internal fun testApplicationContextBuilder(
                embeddedPostgres: EmbeddedPostgres = Companion.embeddedPostgres,
                wireMockServer: WireMockServer = Companion.wireMockServer
        ) = ApplicationContext.Builder(
            env = mapOf(
                "DATABASE_HOST" to "localhost",
                "DATABASE_PORT" to "${embeddedPostgres.port}",
                "DATABASE_DATABASE" to "postgres",
                "DATABASE_USERNAME" to "postgres",
                "DATABASE_PASSWORD" to "postgres",
                "OMSORGSPENGER_TILGANGSSTYRING_BASE_URL" to wireMockServer.tilgangApiBaseUrl(),
                "PDL_BASE_URL" to wireMockServer.pdlApiBaseUrl(),
                "PDL_SCOPES" to "pdl/.default",
                "AZURE_APP_CLIENT_ID" to "omsorgspenger-sak",
                "AZURE_APP_CLIENT_SECRET" to "azureSecret",
                "AZURE_OPENID_CONFIG_ISSUER" to Azure.V2_0.getIssuer(),
                "AZURE_OPENID_CONFIG_JWKS_URI" to wireMockServer.getAzureV2JwksUrl(),
                "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to wireMockServer.getAzureV2TokenUrl()
            )
        )


        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                embeddedPostgres.close()
                wireMockServer.stop()
            })
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
