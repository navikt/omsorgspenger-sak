package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern

private const val pdlApiBasePath = "/pdlapi-mock"
private const val pdlApiMockPath = "/"

private fun WireMockServer.stubPdlApiStandardSvar(): WireMockServer {
    WireMock.stubFor(
            WireMock.post(WireMock
                    .urlPathMatching(".*$pdlApiMockPath.*")).atPriority(9)
                    .withHeader("Authorization", containing("Bearer"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withHeader("Nav-Consumer-Token", AnythingPattern())
                    .withHeader("x-nav-apiKey", AnythingPattern())
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                    {
                                       "data":{
                                       }
                                    }
                            """.trimIndent())
                    )
    )

    return this
}

private fun WireMockServer.stubPdlApiHentIdenterBolk(): WireMockServer {
    WireMock.stubFor(
            WireMock.post(WireMock
                    .urlPathMatching(".*$pdlApiMockPath.*"))
                    .withHeader("Authorization", containing("Bearer"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withHeader("Nav-Consumer-Token", AnythingPattern())
                    .withHeader("x-nav-apiKey", AnythingPattern())
                    .withRequestBody(matchingJsonPath("$.variables.ident", equalTo("[ \"12345678910\", \"12345678911\" ]")))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                    {
                                       "data":{
                                          "hentIdenterBolk":[
                                             {
                                                "ident":"12345678910",
                                                "identer":[
                                                   {
                                                      "ident":"12345678910"
                                                   },
                                                   {
                                                      "ident":"9987654321"
                                                   }
                                                ],
                                                "code":"ok"
                                             },
                                             {
                                                "ident":"12345678911",
                                                "identer":null,
                                                "code":"not_found"
                                             }
                                          ]
                                       }
                                    }
                            """.trimIndent())
                    )
    )

    return this
}

private fun WireMockServer.stubPdlApiHentIdenterBolkUtenInnhold(): WireMockServer {
    WireMock.stubFor(
            WireMock.post(WireMock
                    .urlPathMatching(".*$pdlApiMockPath.*"))
                    .withHeader("Authorization", containing("Bearer"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withHeader("Nav-Consumer-Token", AnythingPattern())
                    .withHeader("x-nav-apiKey", AnythingPattern())
                    .withRequestBody(matchingJsonPath("$.variables.ident", equalTo("[ \"404\" ]")))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                    {
                                       "data":{
                                          "hentIdenterBolk":[
                                             {
                                                "ident":"404",
                                                "identer":null,
                                                "code":"not_found"
                                             }
                                          ]
                                       }
                                    }
                            """.trimIndent())
                    )
    )

    return this
}

private fun WireMockServer.stubPdlApiHentPersonMedTvaIdentOchHistoriskSak(): WireMockServer {
    WireMock.stubFor(
            WireMock.post(WireMock
                    .urlPathMatching(".*$pdlApiMockPath.*"))
                    .withHeader("Authorization", containing("Bearer"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withHeader("Nav-Consumer-Token", AnythingPattern())
                    .withHeader("x-nav-apiKey", AnythingPattern())
                    .withRequestBody(matchingJsonPath("$.variables.ident", equalTo("[ \"01019911111\" ]")))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                    {
                                       "data":{
                                          "hentIdenterBolk":[
                                             {
                                                "ident":"01019911111",
                                                "identer":[
                                                   {
                                                      "ident":"01019911111"
                                                   },
                                                   {
                                                      "ident":"51019911111"
                                                   }
                                                ],
                                                "code":"ok"
                                             }
                                          ]
                                       }
                                    }
                            """.trimIndent())
                    )
    )

    return this
}

private fun WireMockServer.stubPdlApiServerErrorResponse(): WireMockServer {
    WireMock.stubFor(
            WireMock.post(WireMock
                    .urlPathMatching(".*$pdlApiMockPath.*"))
                    .withHeader("Authorization", containing("Bearer"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withHeader("Nav-Consumer-Token", AnythingPattern())
                    .withHeader("x-nav-apiKey", AnythingPattern())
                    .withRequestBody(matchingJsonPath("$.variables.ident", equalTo("[ \"500\" ]")))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(500)
                    )
    )

    return this
}


internal fun WireMockServer.stubPdlApi() = stubPdlApiHentIdenterBolk()
        .stubPdlApiHentIdenterBolkUtenInnhold()
        .stubPdlApiStandardSvar()
        .stubPdlApiServerErrorResponse()
        .stubPdlApiHentPersonMedTvaIdentOchHistoriskSak()

internal fun WireMockServer.pdlApiBaseUrl() = baseUrl() + pdlApiBasePath