package io.codebrews.wiremockdemo

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import java.io.FileNotFoundException
import java.io.IOException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SNIMatcher
import javax.net.ssl.SNIServerName
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLParameters

@Configuration
@Suppress("unused")
class OpenWeatherConfig(@Value("\${app.openweather.baseurl}") val baseUrl: String) {
    val apiKey = System.getenv("OPENWEATHER_API_KEY") ?: "94dd4441acc67062542e6a8948cc584e"

    @Bean
    @Throws(
        UnrecoverableKeyException::class,
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        CertificateException::class,
        FileNotFoundException::class,
        IOException::class
    )
    fun webClientBuilder(): WebClient.Builder {
        val sslContext: SslContext = SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build()

        val httpClient: HttpClient = HttpClient.create()
            .secure {
                it.sslContext(sslContext)
                    .handlerConfigurator {
                        val engine: SSLEngine = it.engine()
                        //engine.setNeedClientAuth(true);
                        val params = SSLParameters()
                        val matchers: MutableList<SNIMatcher> = LinkedList()
                        val matcher: SNIMatcher = object : SNIMatcher(0) {
                            override fun matches(serverName: SNIServerName): Boolean {
                                return true
                            }
                        }
                        matchers.add(matcher)
                        params.sniMatchers = matchers
                        engine.sslParameters = params
                    }
            }
            .proxy {
                it.type(ProxyProvider.Proxy.HTTP).host("ebcswg.bmogc.net").port(8080).username("lbu01").password { "Ryan2021)" }
            }
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofMillis(5000))
            .doOnConnected{ it.addHandlerLast(ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                .addHandlerLast(WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS)) }

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { configurer: ClientCodecConfigurer ->
                configurer
                    .defaultCodecs()
                    .maxInMemorySize(4 * 1024 * 1024)
            }
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }

    @Bean
    fun createWebClient(): WebClient {
        return webClientBuilder().baseUrl(baseUrl).build()
    }
}
