package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.generated.model.FeaturedContentSource;
import com.skateboard.uibackend.client.podcast.PodcastClient;
import com.skateboard.uibackend.client.podcast.generated.model.PostPlatformResponse;
import com.skateboard.uibackend.client.podcast.generated.model.PostResponse;
import com.skateboard.uibackend.dto.HomeFeaturedPlayerResponse;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PodcastFeaturedContentResolverTest {

    @Mock
    private PodcastClient podcastClient;

    private PodcastFeaturedContentResolver resolver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolver = new PodcastFeaturedContentResolver(podcastClient);
    }

    @Test
    void onlySupportsPodcastSource() {
        assertThat(resolver.supports(FeaturedContentSource.PODCAST)).isTrue();
    }

    @Test
    void prefersSpotifyPlaybackWhenAvailable() {
        UUID id = UUID.randomUUID();
        PostResponse post = new PostResponse()
                .id(id).title("Episode 1").coverUrl("cover.png").durationSeconds(120)
                .status(PostResponse.StatusEnum.PUBLISHED)
                .platforms(List.of(
                        new PostPlatformResponse().platform(PostPlatformResponse.PlatformEnum.YOUTUBE).externalUrl("https://youtube.com/watch?v=x"),
                        new PostPlatformResponse().platform(PostPlatformResponse.PlatformEnum.SPOTIFY).externalUrl("https://open.spotify.com/episode/abc")));
        when(podcastClient.getById(id)).thenReturn(post);

        HomeFeaturedPlayerResponse result = resolver.resolve(id.toString(), null);

        assertThat(result.playback().type()).isEqualTo("SPOTIFY_EMBED");
        assertThat(result.playback().reference()).isEqualTo("https://open.spotify.com/episode/abc");
    }

    @Test
    void fallsBackToYoutubePlatformLinkWhenNoSpotify() {
        UUID id = UUID.randomUUID();
        PostResponse post = new PostResponse()
                .id(id).title("Episode 1").status(PostResponse.StatusEnum.PUBLISHED)
                .platforms(List.of(new PostPlatformResponse().platform(PostPlatformResponse.PlatformEnum.YOUTUBE)
                        .externalUrl("https://youtube.com/watch?v=x")));
        when(podcastClient.getById(id)).thenReturn(post);

        HomeFeaturedPlayerResponse result = resolver.resolve(id.toString(), null);

        assertThat(result.playback().type()).isEqualTo("YOUTUBE");
        assertThat(result.playback().reference()).isEqualTo("https://youtube.com/watch?v=x");
    }

    @Test
    void returnsNullWhenPostIsNotPublished() {
        UUID id = UUID.randomUUID();
        PostResponse post = new PostResponse().id(id).title("Draft").status(PostResponse.StatusEnum.DRAFT);
        when(podcastClient.getById(id)).thenReturn(post);

        assertThat(resolver.resolve(id.toString(), null)).isNull();
    }

    @Test
    void returnsNullWhenPostHasNoResolvablePlayback() {
        UUID id = UUID.randomUUID();
        PostResponse post = new PostResponse().id(id).title("No playback").status(PostResponse.StatusEnum.PUBLISHED);
        when(podcastClient.getById(id)).thenReturn(post);

        assertThat(resolver.resolve(id.toString(), null)).isNull();
    }

    @Test
    void returnsNullWhenPostIsMissing() {
        UUID id = UUID.randomUUID();
        when(podcastClient.getById(id)).thenThrow(
                new DownstreamServiceException(HttpStatus.NOT_FOUND, "PODCAST_NOT_FOUND", "Post not found"));

        assertThat(resolver.resolve(id.toString(), null)).isNull();
    }

    @Test
    void returnsNullWhenContentIdIsNotAValidUuid() {
        assertThat(resolver.resolve("not-a-uuid", null)).isNull();
    }

    @Test
    void preferredPlatformOverridesTheDefaultSpotifyPreference() {
        UUID id = UUID.randomUUID();
        PostResponse post = new PostResponse()
                .id(id).title("Episode 1").status(PostResponse.StatusEnum.PUBLISHED)
                .platforms(List.of(
                        new PostPlatformResponse().platform(PostPlatformResponse.PlatformEnum.YOUTUBE).externalUrl("https://youtube.com/watch?v=x"),
                        new PostPlatformResponse().platform(PostPlatformResponse.PlatformEnum.SPOTIFY).externalUrl("https://open.spotify.com/episode/abc")));
        when(podcastClient.getById(id)).thenReturn(post);

        HomeFeaturedPlayerResponse result = resolver.resolve(id.toString(), "YOUTUBE");

        assertThat(result.playback().type()).isEqualTo("YOUTUBE");
        assertThat(result.playback().reference()).isEqualTo("https://youtube.com/watch?v=x");
    }

    @Test
    void unavailablePreferredPlatformFallsBackToWhicheverIsAvailable() {
        UUID id = UUID.randomUUID();
        PostResponse post = new PostResponse()
                .id(id).title("Episode 1").status(PostResponse.StatusEnum.PUBLISHED)
                .platforms(List.of(new PostPlatformResponse().platform(PostPlatformResponse.PlatformEnum.SPOTIFY)
                        .externalUrl("https://open.spotify.com/episode/abc")));
        when(podcastClient.getById(id)).thenReturn(post);

        // Preference asks for YOUTUBE, but this episode only has Spotify —
        // must still resolve rather than returning null.
        HomeFeaturedPlayerResponse result = resolver.resolve(id.toString(), "YOUTUBE");

        assertThat(result.playback().type()).isEqualTo("SPOTIFY_EMBED");
    }
}
