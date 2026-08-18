package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.AppConfigClient;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigMode;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigResponse;
import com.skateboard.uibackend.client.podcast.PodcastClient;
import com.skateboard.uibackend.client.podcast.generated.model.FeedPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.PostResponse;
import com.skateboard.uibackend.dto.HomeVideoResponse;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class HomeServiceTest {

    @Mock
    private AppConfigClient appConfigClient;

    @Mock
    private PodcastClient podcastClient;

    private HomeService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new HomeService(appConfigClient, podcastClient);
    }

    @Test
    void allModePagesThroughTheMainFeedAndFiltersDrafts() {
        when(appConfigClient.getHomeVideoCategoryConfig())
                .thenReturn(new HomeVideoCategoryConfigResponse().mode(HomeVideoCategoryConfigMode.ALL).enabledCategoryIds(List.of()));

        PostResponse published = post("Published Video", PostResponse.StatusEnum.PUBLISHED);
        PostResponse draft = post("Draft Video", PostResponse.StatusEnum.DRAFT);
        when(podcastClient.getFeed(eq(0), anyInt()))
                .thenReturn(new FeedPageResponse().posts(List.of(published, draft)).page(0).size(50).total(2L));

        List<HomeVideoResponse> videos = service.getVideos();

        assertThat(videos).hasSize(1);
        assertThat(videos.get(0).title()).isEqualTo("Published Video");
        assertThat(videos.get(0).category()).isNull();
    }

    @Test
    void allModeLoopsUntilTheLastPage() {
        when(appConfigClient.getHomeVideoCategoryConfig())
                .thenReturn(new HomeVideoCategoryConfigResponse().mode(HomeVideoCategoryConfigMode.ALL).enabledCategoryIds(List.of()));

        PostResponse first = post("First", PostResponse.StatusEnum.PUBLISHED);
        PostResponse second = post("Second", PostResponse.StatusEnum.PUBLISHED);
        when(podcastClient.getFeed(eq(0), anyInt()))
                .thenReturn(new FeedPageResponse().posts(List.of(first)).page(0).size(1).total(2L));
        when(podcastClient.getFeed(eq(1), anyInt()))
                .thenReturn(new FeedPageResponse().posts(List.of(second)).page(1).size(1).total(2L));

        List<HomeVideoResponse> videos = service.getVideos();

        assertThat(videos).extracting(HomeVideoResponse::title).containsExactlyInAnyOrder("First", "Second");
    }

    @Test
    void selectedModeMergesAndDedupesAcrossCategories() {
        when(appConfigClient.getHomeVideoCategoryConfig()).thenReturn(new HomeVideoCategoryConfigResponse()
                .mode(HomeVideoCategoryConfigMode.SELECTED).enabledCategoryIds(List.of("podcasts", "skate-clips")));

        UUID sharedId = UUID.randomUUID();
        PostResponse shared = post("Shared", PostResponse.StatusEnum.PUBLISHED).id(sharedId);
        PostResponse onlyInSecond = post("Second Only", PostResponse.StatusEnum.PUBLISHED);

        when(podcastClient.getCategoryPosts(eq("podcasts"), eq(0), anyInt()))
                .thenReturn(new FeedPageResponse().posts(List.of(shared)).page(0).size(50).total(1L));
        when(podcastClient.getCategoryPosts(eq("skate-clips"), eq(0), anyInt()))
                .thenReturn(new FeedPageResponse().posts(List.of(shared, onlyInSecond)).page(0).size(50).total(2L));

        List<HomeVideoResponse> videos = service.getVideos();

        assertThat(videos).extracting(HomeVideoResponse::title).containsExactlyInAnyOrder("Shared", "Second Only");
    }

    @Test
    void selectedModeSkipsAStaleCategorySlugInsteadOfFailing() {
        when(appConfigClient.getHomeVideoCategoryConfig()).thenReturn(new HomeVideoCategoryConfigResponse()
                .mode(HomeVideoCategoryConfigMode.SELECTED).enabledCategoryIds(List.of("deleted-category", "podcasts")));

        when(podcastClient.getCategoryPosts(eq("deleted-category"), eq(0), anyInt()))
                .thenThrow(new DownstreamServiceException(HttpStatus.NOT_FOUND, "PODCAST_NOT_FOUND", "Category not found"));
        PostResponse valid = post("Valid", PostResponse.StatusEnum.PUBLISHED);
        when(podcastClient.getCategoryPosts(eq("podcasts"), eq(0), anyInt()))
                .thenReturn(new FeedPageResponse().posts(List.of(valid)).page(0).size(50).total(1L));

        List<HomeVideoResponse> videos = service.getVideos();

        assertThat(videos).extracting(HomeVideoResponse::title).containsExactly("Valid");
    }

    @Test
    void fallsBackToAllWhenTheConfigServiceIsUnavailable() {
        when(appConfigClient.getHomeVideoCategoryConfig())
                .thenThrow(new DownstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "APP_CONFIG_SERVICE_UNAVAILABLE", "down"));

        PostResponse published = post("Fallback Video", PostResponse.StatusEnum.PUBLISHED);
        when(podcastClient.getFeed(eq(0), anyInt()))
                .thenReturn(new FeedPageResponse().posts(List.of(published)).page(0).size(50).total(1L));

        List<HomeVideoResponse> videos = service.getVideos();

        assertThat(videos).extracting(HomeVideoResponse::title).containsExactly("Fallback Video");
    }

    @Test
    void mapsCoverDimensionsThroughToTheThumbnailFields() {
        when(appConfigClient.getHomeVideoCategoryConfig())
                .thenReturn(new HomeVideoCategoryConfigResponse().mode(HomeVideoCategoryConfigMode.ALL).enabledCategoryIds(List.of()));

        PostResponse sized = post("Sized", PostResponse.StatusEnum.PUBLISHED).coverWidth(1280).coverHeight(720);
        // Videos whose dimensions were never captured stay null so the client
        // knows to fall back to probing rather than assuming a ratio.
        PostResponse unsized = post("Unsized", PostResponse.StatusEnum.PUBLISHED);
        when(podcastClient.getFeed(eq(0), anyInt()))
                .thenReturn(new FeedPageResponse().posts(List.of(sized, unsized)).page(0).size(50).total(2L));

        List<HomeVideoResponse> videos = service.getVideos();

        assertThat(videos).extracting(HomeVideoResponse::thumbnailWidth).containsExactly(1280, null);
        assertThat(videos).extracting(HomeVideoResponse::thumbnailHeight).containsExactly(720, null);
    }

    private PostResponse post(String title, PostResponse.StatusEnum status) {
        return new PostResponse()
                .id(UUID.randomUUID())
                .slug(title.toLowerCase().replace(" ", "-"))
                .title(title)
                .status(status)
                .coverUrl("https://example.com/cover.png")
                .youtubeVideoId(null);
    }
}
