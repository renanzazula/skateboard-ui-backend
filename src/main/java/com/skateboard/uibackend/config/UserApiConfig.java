package com.skateboard.uibackend.config;

import com.skateboard.uibackend.client.user.generated.api.MeApi;
import com.skateboard.uibackend.client.user.generated.invoker.ApiClient;
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
 * Wires the openapi-generator-produced {@link MeApi} (generated from the
 * "me" tag in api/user-openapi.yaml, hence the class name) to a WebClient
 * configured with skateboard-user-be's base URL/timeouts and the two
 * cross-cutting filters (bearer token relay, correlation id propagation).
 * Mirrors {@link PodcastApiConfig} — see its javadoc for why
 * {@code webClientBuilder} (not a raw {@code WebClient.builder()}) and a
 * separately-set {@link ApiClient#setBasePath} are both required.
 */
@Configuration
public class UserApiConfig {

    @Bean
    public MeApi meApi(WebClient.Builder webClientBuilder,
                        ClientsProperties clientsProperties,
                        BearerTokenExchangeFilter bearerTokenExchangeFilter,
                        CorrelationIdExchangeFilter correlationIdExchangeFilter) {
        ClientsProperties.User config = clientsProperties.getUser();

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

        return new MeApi(apiClient);
    }
}
