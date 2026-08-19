package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.AppConfigClient;
import com.skateboard.uibackend.client.appconfig.generated.model.FeaturedContentSource;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeFeaturedPlayerConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.HomePlayerPosition;
import com.skateboard.uibackend.client.appconfig.generated.model.HomePlayerType;
import com.skateboard.uibackend.dto.HomeFeaturedPlayerResponse;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class HomeFeaturedPlayerServiceTest {

    @Mock
    private AppConfigClient appConfigClient;

    @Mock
    private FeaturedContentResolver resolver;

    private HomeFeaturedPlayerService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new HomeFeaturedPlayerService(appConfigClient, List.of(resolver));
    }

    @Test
    void returnsNullWhenDisabled() {
        when(appConfigClient.getHomeFeaturedPlayerConfig()).thenReturn(new HomeFeaturedPlayerConfigResponse()
                .enabled(false).contentSource(FeaturedContentSource.PODCAST).contentId("post-1")
                .playerType(HomePlayerType.MINI).position(HomePlayerPosition.BOTTOM));

        assertThat(service.getFeaturedPlayer()).isNull();
    }

    @Test
    void returnsNullWhenEnabledButNoContentConfigured() {
        when(appConfigClient.getHomeFeaturedPlayerConfig()).thenReturn(new HomeFeaturedPlayerConfigResponse()
                .enabled(true).contentSource(null).contentId(null)
                .playerType(HomePlayerType.MINI).position(HomePlayerPosition.BOTTOM));

        assertThat(service.getFeaturedPlayer()).isNull();
    }

    @Test
    void resolvesContentThroughTheMatchingResolverAndFillsInPositionFromConfig() {
        when(appConfigClient.getHomeFeaturedPlayerConfig()).thenReturn(new HomeFeaturedPlayerConfigResponse()
                .enabled(true).contentSource(FeaturedContentSource.PODCAST).contentId("post-1")
                .playerType(HomePlayerType.MINI).position(HomePlayerPosition.BOTTOM));
        when(resolver.supports(FeaturedContentSource.PODCAST)).thenReturn(true);
        HomeFeaturedPlayerResponse resolved = new HomeFeaturedPlayerResponse("post-1", "PODCAST", "Ep 1", "Skateboard Podcast",
                "cover.png", 100, new HomeFeaturedPlayerResponse.Playback("SPOTIFY_EMBED", "https://open.spotify.com/episode/abc"), null);
        when(resolver.resolve("post-1", null)).thenReturn(resolved);

        HomeFeaturedPlayerResponse result = service.getFeaturedPlayer();

        assertThat(result.id()).isEqualTo("post-1");
        assertThat(result.position()).isEqualTo("BOTTOM");
    }

    @Test
    void returnsNullWhenNoResolverSupportsTheConfiguredSource() {
        when(appConfigClient.getHomeFeaturedPlayerConfig()).thenReturn(new HomeFeaturedPlayerConfigResponse()
                .enabled(true).contentSource(FeaturedContentSource.PODCAST).contentId("post-1")
                .playerType(HomePlayerType.MINI).position(HomePlayerPosition.BOTTOM));
        when(resolver.supports(FeaturedContentSource.PODCAST)).thenReturn(false);

        assertThat(service.getFeaturedPlayer()).isNull();
    }

    @Test
    void returnsNullWhenAppConfigIsUnavailable() {
        when(appConfigClient.getHomeFeaturedPlayerConfig())
                .thenThrow(new DownstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "APP_CONFIG_SERVICE_UNAVAILABLE", "down"));

        assertThat(service.getFeaturedPlayer()).isNull();
    }
}
