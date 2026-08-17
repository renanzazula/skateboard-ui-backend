package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.AppConfigClient;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigMode;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigResponse;
import com.skateboard.uibackend.client.podcast.PodcastClient;
import com.skateboard.uibackend.client.podcast.generated.model.FeedPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.PostResponse;
import com.skateboard.uibackend.dto.HomeVideoResponse;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregates the Home dashboard's video list: reads the eligible-category
 * configuration from skateboard-app-config-be and the matching videos from
 * skateboard-podcast-be. Returns a flat, unshuffled list — randomization is a
 * frontend concern (README_HOME_DASHBOARD.md §4/§23).
 * <p>
 * podcast-be has no unbounded "all posts"/multi-category endpoint, so ALL
 * mode pages through the main feed and SELECTED mode pages through each
 * chosen category in turn, merging by post id. Fine for a first version;
 * revisit (batch endpoint or caching here) if the catalog grows large enough
 * to make the per-request round trips expensive.
 */
@Service
public class HomeService {

    private static final Logger log = LoggerFactory.getLogger(HomeService.class);
    // podcast-be's controller clamps size to 50 (PodcastController#getPodcastFeed)
    // regardless of what's requested — asking for exactly that avoids
    // requesting a value that's silently downgraded.
    private static final int PAGE_SIZE = 50;

    private final AppConfigClient appConfigClient;
    private final PodcastClient podcastClient;

    public HomeService(AppConfigClient appConfigClient, PodcastClient podcastClient) {
        this.appConfigClient = appConfigClient;
        this.podcastClient = podcastClient;
    }

    public List<HomeVideoResponse> getVideos() {
        HomeVideoCategoryConfigResponse config = loadConfigOrFallbackToAll();

        Map<UUID, HomeVideoResponse> videosById = new LinkedHashMap<>();
        if (config.getMode() == HomeVideoCategoryConfigMode.SELECTED) {
            for (String slug : config.getEnabledCategoryIds()) {
                collectCategoryPosts(slug, videosById);
            }
        } else {
            collectFeedPosts(videosById);
        }
        return new ArrayList<>(videosById.values());
    }

    // skateboard-app-config-be being unavailable must not blank the Home
    // dashboard — fall back to ALL categories (README_HOME_DASHBOARD.md §22.7).
    private HomeVideoCategoryConfigResponse loadConfigOrFallbackToAll() {
        try {
            return appConfigClient.getHomeVideoCategoryConfig();
        } catch (DownstreamServiceException ex) {
            log.warn("Home category config unavailable ({}); falling back to ALL categories", ex.getCode());
            return new HomeVideoCategoryConfigResponse().mode(HomeVideoCategoryConfigMode.ALL).enabledCategoryIds(List.of());
        }
    }

    private void collectFeedPosts(Map<UUID, HomeVideoResponse> videosById) {
        int page = 0;
        FeedPageResponse feed;
        do {
            feed = podcastClient.getFeed(page, PAGE_SIZE);
            addPublished(feed.getPosts(), null, videosById);
            page++;
        } while (!isLastPage(feed));
    }

    // A category referenced by the config but since deleted in podcast-be
    // must not break the whole dashboard — skip it (README_HOME_DASHBOARD.md §22.6).
    private void collectCategoryPosts(String slug, Map<UUID, HomeVideoResponse> videosById) {
        int page = 0;
        FeedPageResponse feed;
        do {
            try {
                feed = podcastClient.getCategoryPosts(slug, page, PAGE_SIZE);
            } catch (DownstreamServiceException ex) {
                if ("PODCAST_NOT_FOUND".equals(ex.getCode())) {
                    log.warn("Home config references unknown category slug '{}'; skipping", slug);
                    return;
                }
                throw ex;
            }
            addPublished(feed.getPosts(), slug, videosById);
            page++;
        } while (!isLastPage(feed));
    }

    private void addPublished(List<PostResponse> posts, String categorySlug, Map<UUID, HomeVideoResponse> videosById) {
        for (PostResponse post : posts) {
            if (post.getStatus() != PostResponse.StatusEnum.PUBLISHED) {
                continue;
            }
            videosById.putIfAbsent(post.getId(),
                    new HomeVideoResponse(post.getId(), post.getSlug(), post.getTitle(), post.getCoverUrl(), post.getYoutubeVideoId(), categorySlug));
        }
    }

    private boolean isLastPage(FeedPageResponse feed) {
        long fetched = (long) (feed.getPage() + 1) * feed.getSize();
        return feed.getPosts().isEmpty() || fetched >= feed.getTotal();
    }
}
