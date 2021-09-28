package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath

private const val pdlApiBasePath = "/pdlapi-mock"
private const val pdlApiMockPath = "/"

data class IdentHistorikk(
    val gjeldende: String,
    val historiske: List<String>
)

internal object PdlEnFinnesEnFinnesIkke {
    val finnes = IdentHistorikk(
        gjeldende = "12345678910",
        historiske = listOf("9987654321", "12345678910")
    )
    val finnesIkke = "12345678911"
}

val pdlIdentMedHistorikk = IdentHistorikk(
    gjeldende = "01019911111",
    historiske = listOf("01019911111", "51019911111")
)

val pdlIdentIngenHistorikk_1 = "11111111111"
val pdlIdentIngenHistorikk_2 = "11111111112"
val pdlIdentIngenHistorikk_3 = "01111111115"

private fun WireMockServer.stubPdlApiHentIdenterBolk(): WireMockServer {
    val finnes = PdlEnFinnesEnFinnesIkke.finnes
    val finnesIkke = PdlEnFinnesEnFinnesIkke.finnesIkke
    WireMock.stubFor(
            WireMock.post(WireMock
                    .urlPathMatching(".*$pdlApiMockPath.*"))
                    .withHeader("Authorization", containing("Bearer"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withRequestBody(matchingJsonPath("$.variables.identer", containing("${finnes.gjeldende}")))
                    .withRequestBody(matchingJsonPath("$.variables.identer", containing("$finnesIkke")))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                    {
                                       "data":{
                                          "hentIdenterBolk":[
                                             {
                                                "ident":"${finnes.gjeldende}",
                                                "identer":[
                                                   {
                                                      "ident":"${finnes.gjeldende}"
                                                   },
                                                   {
                                                      "ident":"${finnes.historiske[0]}"
                                                   }
                                                ],
                                                "code":"ok"
                                             },
                                             {
                                                "ident":"$finnesIkke",
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
                    .withRequestBody(matchingJsonPath("$.variables.identer", equalToJson("[ \"404\" ]")))
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
                    .withRequestBody(matchingJsonPath("$.variables.identer", equalTo("${pdlIdentMedHistorikk.gjeldende}")))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                    {
                                       "data":{
                                          "hentIdenterBolk":[
                                             {
                                                "ident":"${pdlIdentMedHistorikk.gjeldende}",
                                                "identer":[
                                                   {
                                                      "ident":"${pdlIdentMedHistorikk.gjeldende}"
                                                   },
                                                   {
                                                      "ident":"${pdlIdentMedHistorikk.historiske[1]}"
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

private fun WireMockServer.stubIdenterIngenHistoriske(vararg identerVarArgs: String): WireMockServer {
    val identer = identerVarArgs.asList()
    val identerString = identer.joinToString(separator = ",") { "\"$it\"" }
    val identResponseJson = """
        {
            "ident":"{ident}",
            "identer":[
               {
                  "ident":"{ident}"
               }
            ],
            "code":"ok"
        }
    """.trimIndent()
    val identerMapString = identer.joinToString { identResponseJson.replace("{ident}", it) }
    WireMock.stubFor(
        WireMock.post(WireMock
            .urlPathMatching(".*$pdlApiMockPath.*"))
            .withHeader("Authorization", containing("Bearer"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.variables.identer", equalToJson("[$identerString]")))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                           "data":{
                              "hentIdenterBolk":[$identerMapString]
                           }
                        }
                        """.trimIndent()
                    )
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
                    .withRequestBody(matchingJsonPath("$.variables.identer", equalToJson("[ \"500\" ]")))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(500)
                    )
    )

    return this
}

private fun WireMockServer.stubPdlApiHealthCheck(): WireMockServer {
    WireMock.stubFor(
            WireMock.options(WireMock
                    .urlPathMatching(".*$pdlApiMockPath.*"))
                    .withHeader("Authorization", containing("Bearer"))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                    )
    )

    return this
}

internal fun WireMockServer.stubPdlApi() = stubPdlApiHentIdenterBolk()
    .stubPdlApiHentIdenterBolkUtenInnhold()
    .stubPdlApiServerErrorResponse()
    .stubPdlApiHentPersonMedTvaIdentOchHistoriskSak()
    .stubPdlApiHealthCheck()
    .stubIdenterIngenHistoriske(pdlIdentIngenHistorikk_1)
    .stubIdenterIngenHistoriske(pdlIdentIngenHistorikk_2)
    .stubIdenterIngenHistoriske(pdlIdentIngenHistorikk_1, pdlIdentIngenHistorikk_2)
    .stubIdenterIngenHistoriske(pdlIdentIngenHistorikk_3)
    .stubIdenterIngenHistoriske(pdlIdentIngenHistorikk_1, pdlIdentIngenHistorikk_3)


internal fun WireMockServer.pdlApiBaseUrl() = baseUrl() + pdlApiBasePath
