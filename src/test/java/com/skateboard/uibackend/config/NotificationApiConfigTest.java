package com.skateboard.uibackend.config;

import com.skateboard.uibackend.client.notification.generated.api.DevicesApi;
import com.skateboard.uibackend.client.notification.generated.api.PreferencesApi;
import com.skateboard.uibackend.client.notification.generated.invoker.ApiClient;
import com.skateboard.uibackend.web.BearerTokenExchangeFilter;
import com.skateboard.uibackend.web.CorrelationIdExchangeFilter;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/** Mirrors {@link AppConfigApiConfigTest}'s shape. */
class NotificationApiConfigTest {

    private final NotificationApiConfig config = new NotificationApiConfig();

    @Test
    void wiresTheApiClientToTheConfiguredBaseUrl() {
        ClientsProperties properties = new ClientsProperties();
        properties.getNotification().setBaseUrl("http://notification-be");
        properties.getNotification().setConnectTimeoutMs(1000);
        properties.getNotification().setReadTimeoutMs(2000);

        ApiClient apiClient = config.notificationApiClient(WebClient.builder(), properties,
                new BearerTokenExchangeFilter(), new CorrelationIdExchangeFilter());

        assertThat(apiClient.getBasePath()).isEqualTo("http://notification-be");
    }

    @Test
    void wiresEachGeneratedApiToTheSharedApiClient() {
        ApiClient apiClient = new ApiClient();

        assertThat(config.notificationDevicesApi(apiClient)).isInstanceOf(DevicesApi.class);
        assertThat(config.notificationPreferencesApi(apiClient)).isInstanceOf(PreferencesApi.class);
    }
}
