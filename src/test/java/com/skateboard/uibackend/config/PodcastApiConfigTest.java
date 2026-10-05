package com.skateboard.uibackend.config;

import com.skateboard.uibackend.client.podcast.generated.api.GuestApplicationsApi;
import com.skateboard.uibackend.client.podcast.generated.api.PodcastApi;
import com.skateboard.uibackend.client.podcast.generated.invoker.ApiClient;
import com.skateboard.uibackend.web.BearerTokenExchangeFilter;
import com.skateboard.uibackend.web.CorrelationIdExchangeFilter;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/** Mirrors {@link AppConfigApiConfigTest}'s shape. */
class PodcastApiConfigTest {

    private final PodcastApiConfig config = new PodcastApiConfig();

    @Test
    void wiresTheApiClientToTheConfiguredBaseUrl() {
        ClientsProperties properties = new ClientsProperties();
        properties.getPodcast().setBaseUrl("http://podcast-be");
        properties.getPodcast().setConnectTimeoutMs(1000);
        properties.getPodcast().setReadTimeoutMs(2000);

        ApiClient apiClient = config.podcastApiClient(WebClient.builder(), properties,
                new BearerTokenExchangeFilter(), new CorrelationIdExchangeFilter());

        assertThat(apiClient.getBasePath()).isEqualTo("http://podcast-be");
    }

    @Test
    void wiresEachGeneratedApiToTheSharedApiClient() {
        ApiClient apiClient = new ApiClient();

        assertThat(config.podcastApi(apiClient)).isInstanceOf(PodcastApi.class);
        assertThat(config.guestApplicationsApi(apiClient)).isInstanceOf(GuestApplicationsApi.class);
    }
}
