package com.skateboard.uibackend.config;

import com.skateboard.uibackend.client.user.generated.api.MeApi;
import com.skateboard.uibackend.web.BearerTokenExchangeFilter;
import com.skateboard.uibackend.web.CorrelationIdExchangeFilter;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/** Mirrors {@link AppConfigApiConfigTest}'s shape. */
class UserApiConfigTest {

    private final UserApiConfig config = new UserApiConfig();

    @Test
    void wiresTheApiClientToTheConfiguredBaseUrl() {
        ClientsProperties properties = new ClientsProperties();
        properties.getUser().setBaseUrl("http://user-be");
        properties.getUser().setConnectTimeoutMs(1000);
        properties.getUser().setReadTimeoutMs(2000);

        MeApi meApi = config.meApi(WebClient.builder(), properties,
                new BearerTokenExchangeFilter(), new CorrelationIdExchangeFilter());

        assertThat(meApi.getApiClient().getBasePath()).isEqualTo("http://user-be");
    }
}
