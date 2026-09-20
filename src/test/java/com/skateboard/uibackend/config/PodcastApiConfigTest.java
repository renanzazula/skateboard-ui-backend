package com.skateboard.uibackend.config;

import com.skateboard.uibackend.client.podcast.generated.api.PodcastApi;
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

        PodcastApi podcastApi = config.podcastApi(WebClient.builder(), properties,
                new BearerTokenExchangeFilter(), new CorrelationIdExchangeFilter());

        assertThat(podcastApi.getApiClient().getBasePath()).isEqualTo("http://podcast-be");
    }
}
