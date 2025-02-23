package io.codebrews.wiremockdemo

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@SpringBootTest
@ContextConfiguration(initializers = [WireMockContextInitializer::class])
@AutoConfigureWebTestClient
class RouteTest {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @Autowired
    private lateinit var client: WebTestClient

    @AfterEach
    fun afterEach() {
        wireMockServer.resetAll()
    }

    private fun stubResponse(url: String, responseBody: String, responseStatus: Int = HttpStatus.OK.value()) {
        wireMockServer.stubFor(get(url)
            .willReturn(
                aResponse()
                .withStatus(responseStatus)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(responseBody))
        )
    }

    private val apiResponseFileName = "openweather-api-response.json"
    private val openWeatherApiResponse: String? = this::class.java.classLoader.getResource(apiResponseFileName)?.readText()

    @Test
    fun `test open weather api response is loaded`(){
        logger.info(openWeatherApiResponse)

        assert(openWeatherApiResponse != null)
    }

    @Test
    fun `post route should return 200 with weather information`() {
        val apiKey = System.getenv("OPENWEATHER_API_KEY")
        val cityId = "5174095"
        val url = "/weather?id=$cityId&APPID=$apiKey"

        stubResponse(url, openWeatherApiResponse!!)

        val requestBody = CityId(cityId)
        val responseBody = """
            {
                "main": {
                    "temp": 21.78,
                    "pressure": 1016,
                    "humidity": 88,
                    "temp_min": 20,
                    "temp_max": 23
                }
            }
        """.trimIndent()

        client.post()
            .uri("/api/current-weather")
            .body(Mono.just(requestBody), CityId::class.java)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(responseBody)

        wireMockServer.verify(getRequestedFor(urlEqualTo(url)))
    }
}
