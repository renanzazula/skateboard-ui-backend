package com.skateboard.uibackend.config;

import com.skateboard.uibackend.client.notification.generated.api.DevicesApi;
import com.skateboard.uibackend.client.notification.generated.api.PreferencesApi;
import com.skateboard.uibackend.client.notification.generated.invoker.ApiClient;
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
 * Wires the openapi-generator-produced {@link DevicesApi}/{@link PreferencesApi}
 * (generated from api/notification-openapi.yaml's "devices"/"preferences" tags)
 * to a WebClient configured with skateboard-notification-be's base
 * URL/timeouts and the two cross-cutting filters (bearer token relay,
 * correlation id propagation). Mirrors {@link AppConfigApiConfig}, which is
 * the shape to follow when a downstream spec has more than one tag — see
 * {@link PodcastApiConfig}'s javadoc for why {@code webClientBuilder} (not a
 * raw {@code WebClient.builder()}) and a separately-set
 * {@link ApiClient#setBasePath} are both required.
 */
@Configuration
public class NotificationApiConfig {

    @Bean
    public ApiClient notificationApiClient(WebClient.Builder webClientBuilder,
                                            ClientsProperties clientsProperties,
                                            BearerTokenExchangeFilter bearerTokenExchangeFilter,
                                            CorrelationIdExchangeFilter correlationIdExchangeFilter) {
        ClientsProperties.Notification config = clientsProperties.getNotification();

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
        return apiClient;
    }

    @Bean
    public DevicesApi notificationDevicesApi(ApiClient notificationApiClient) {
        return new DevicesApi(notificationApiClient);
    }

    @Bean
    public PreferencesApi notificationPreferencesApi(ApiClient notificationApiClient) {
        return new PreferencesApi(notificationApiClient);
    }
}
