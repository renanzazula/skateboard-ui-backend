package com.skateboard.uibackend.config;

import com.skateboard.uibackend.client.podcast.generated.api.PodcastApi;
import com.skateboard.uibackend.client.podcast.generated.invoker.ApiClient;
import com.skateboard.uibackend.web.BearerTokenExchangeFilter;
import com.skateboard.uibackend.web.CorrelationIdExchangeFilter;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Wires the openapi-generator-produced {@link PodcastApi} to a WebClient
 * configured with skateboard-podcast-be's base URL/timeouts and the two
 * cross-cutting filters (bearer token relay, correlation id propagation).
 * {@code webClientBuilder} is Spring Boot's autoconfigured
 * {@code WebClient.Builder} (present because spring-webflux is on the
 * classpath for WebClient itself), which already wires the app's Jackson
 * {@code ObjectMapper} (JavaTimeModule etc.) into the codecs — building a raw
 * {@code WebClient.builder()} here instead would silently drop that.
 * <p>
 * {@link ApiClient#setBasePath} is required separately from the WebClient's
 * own base URL: the generated {@code invokeAPI} always resolves request URIs
 * against {@code ApiClient.basePath}, not against anything configured on the
 * WebClient instance itself.
 */
@Configuration
public class PodcastApiConfig {

    @Bean
    public PodcastApi podcastApi(WebClient.Builder webClientBuilder,
                                  ClientsProperties clientsProperties,
                                  BearerTokenExchangeFilter bearerTokenExchangeFilter,
                                  CorrelationIdExchangeFilter correlationIdExchangeFilter) {
        ClientsProperties.Podcast config = clientsProperties.getPodcast();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(config.getReadTimeoutMs()));

        WebClient webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(bearerTokenExchangeFilter)
                .filter(correlationIdExchangeFilter)
                .build();

        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(config.getBaseUrl());

        return new PodcastApi(apiClient);
    }
}
