package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.generated.model.FeaturedContentSource;
import com.skateboard.uibackend.client.podcast.PodcastClient;
import com.skateboard.uibackend.client.podcast.generated.model.PostPlatformResponse;
import com.skateboard.uibackend.client.podcast.generated.model.PostResponse;
import com.skateboard.uibackend.dto.HomeFeaturedPlayerResponse;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Resolves PODCAST-sourced Featured Player content against
 * skateboard-podcast-be. Prefers a SPOTIFY platform link (matching this
 * feature's "Spotify-style mini player" framing), falls back to YOUTUBE, then
 * to the post's legacy {@code youtubeVideoId}/{@code youtubeUrl} fields for
 * posts ingested before platform links existed.
 */
@Component
public class PodcastFeaturedContentResolver implements FeaturedContentResolver {

    private static final Logger log = LoggerFactory.getLogger(PodcastFeaturedContentResolver.class);
    private static final String SUBTITLE = "Skateboard Podcast";

    private final PodcastClient podcastClient;

    public PodcastFeaturedContentResolver(PodcastClient podcastClient) {
        this.podcastClient = podcastClient;
    }

    @Override
    public boolean supports(FeaturedContentSource source) {
        return source == FeaturedContentSource.PODCAST;
    }

    @Override
    public HomeFeaturedPlayerResponse resolve(String contentId) {
        PostResponse post = loadPost(contentId);
        if (post == null) {
            return null;
        }
        if (post.getStatus() != PostResponse.StatusEnum.PUBLISHED) {
            log.warn("Home Featured Player references unpublished/removed post id={}", contentId);
            return null;
        }
        HomeFeaturedPlayerResponse.Playback playback = resolvePlayback(post);
        if (playback == null) {
            log.warn("Home Featured Player references post id={} with no resolvable playback", contentId);
            return null;
        }
        // position is filled in by HomeFeaturedPlayerService from the config
        // response — the resolver only knows about the content itself.
        return new HomeFeaturedPlayerResponse(post.getId().toString(), FeaturedContentSource.PODCAST.getValue(),
                post.getTitle(), SUBTITLE, post.getCoverUrl(), post.getDurationSeconds(), playback, null);
    }

    // A configured contentId that no longer resolves (deleted post, or one
    // that predates this feature and isn't even a UUID) must not fail the
    // whole Home request (README-home-featured-mini-player.md §14) — treat
    // it the same as "not found".
    private PostResponse loadPost(String contentId) {
        UUID id;
        try {
            id = UUID.fromString(contentId);
        } catch (IllegalArgumentException ex) {
            log.warn("Home Featured Player contentId='{}' is not a valid podcast post id", contentId);
            return null;
        }
        try {
            return podcastClient.getById(id);
        } catch (DownstreamServiceException ex) {
            if ("PODCAST_NOT_FOUND".equals(ex.getCode())) {
                log.warn("Home Featured Player references missing podcast post id={}", contentId);
                return null;
            }
            throw ex;
        }
    }

    private HomeFeaturedPlayerResponse.Playback resolvePlayback(PostResponse post) {
        List<PostPlatformResponse> platforms = post.getPlatforms();
        PostPlatformResponse spotify = findPlatform(platforms, PostPlatformResponse.PlatformEnum.SPOTIFY);
        if (spotify != null && spotify.getExternalUrl() != null) {
            return new HomeFeaturedPlayerResponse.Playback("SPOTIFY_EMBED", spotify.getExternalUrl());
        }
        PostPlatformResponse youtube = findPlatform(platforms, PostPlatformResponse.PlatformEnum.YOUTUBE);
        if (youtube != null && youtube.getExternalUrl() != null) {
            return new HomeFeaturedPlayerResponse.Playback("YOUTUBE", youtube.getExternalUrl());
        }
        if (post.getYoutubeUrl() != null) {
            return new HomeFeaturedPlayerResponse.Playback("YOUTUBE", post.getYoutubeUrl());
        }
        return null;
    }

    private PostPlatformResponse findPlatform(List<PostPlatformResponse> platforms, PostPlatformResponse.PlatformEnum platform) {
        if (platforms == null) {
            return null;
        }
        return platforms.stream().filter(p -> p.getPlatform() == platform).findFirst().orElse(null);
    }
}
