package com.skateboard.uibackend.client.podcast;

import com.skateboard.uibackend.client.podcast.generated.api.PodcastApi;
import com.skateboard.uibackend.client.podcast.generated.model.AdminCategoryResponse;
import com.skateboard.uibackend.client.podcast.generated.model.CategoryResponse;
import com.skateboard.uibackend.client.podcast.generated.model.CreatePostRequest;
import com.skateboard.uibackend.client.podcast.generated.model.FeedPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.ImportPostsRequest;
import com.skateboard.uibackend.client.podcast.generated.model.ImportResult;
import com.skateboard.uibackend.client.podcast.generated.model.PostResponse;
import com.skateboard.uibackend.client.podcast.generated.model.ReorderCategoriesRequest;
import com.skateboard.uibackend.client.podcast.generated.model.SyncResultResponse;
import com.skateboard.uibackend.client.podcast.generated.model.UpdateCategoryRequest;
import com.skateboard.uibackend.client.podcast.generated.model.UpdatePostRequest;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Every {@link PodcastClient} passthrough method, plus its shared
 * downstream-error mapping (mirrors {@link
 * com.skateboard.uibackend.client.appconfig.CampaignClientTest}'s shape).
 */
class PodcastClientTest {

    @Mock
    private PodcastApi podcastApi;

    private PodcastClient client;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new PodcastClient(podcastApi);
    }

    private static WebClientResponseException responseException(HttpStatus status) {
        return WebClientResponseException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8, null);
    }

    @Test
    void getFeedPassesThrough() {
        FeedPageResponse response = new FeedPageResponse().page(0).size(10).total(1L);
        when(podcastApi.getPodcastFeed(0, 10, "board")).thenReturn(Mono.just(response));

        assertThat(client.getFeed(0, 10, "board")).isSameAs(response);
    }

    @Test
    void getBySlugPassesThrough() {
        PostResponse response = new PostResponse().id(id).slug("a-post");
        when(podcastApi.getPodcastPostBySlug("a-post")).thenReturn(Mono.just(response));

        assertThat(client.getBySlug("a-post")).isSameAs(response);
    }

    @Test
    void getByIdPassesThrough() {
        PostResponse response = new PostResponse().id(id);
        when(podcastApi.getPodcastPostById(id)).thenReturn(Mono.just(response));

        assertThat(client.getById(id)).isSameAs(response);
    }

    @Test
    void getFeaturedEpisodePassesThroughASuccessfulResult() {
        PostResponse response = new PostResponse().id(id);
        when(podcastApi.getFeaturedEpisode()).thenReturn(Mono.just(response));

        assertThat(client.getFeaturedEpisode()).isSameAs(response);
    }

    @Test
    void getFeaturedEpisodeSwallowsANotFoundIntoNull() {
        when(podcastApi.getFeaturedEpisode()).thenReturn(Mono.error(responseException(HttpStatus.NOT_FOUND)));

        assertThat(client.getFeaturedEpisode()).isNull();
    }

    @Test
    void getFeaturedEpisodeStillPropagatesAGenuineOutage() {
        when(podcastApi.getFeaturedEpisode()).thenReturn(Mono.error(responseException(HttpStatus.INTERNAL_SERVER_ERROR)));

        assertThatThrownBy(() -> client.getFeaturedEpisode())
                .isInstanceOf(DownstreamServiceException.class)
                .satisfies(t -> assertThat(((DownstreamServiceException) t).getCode())
                        .isEqualTo("PODCAST_SERVICE_UNAVAILABLE"));
    }

    @Test
    void createPassesThrough() {
        CreatePostRequest request = new CreatePostRequest();
        PostResponse response = new PostResponse().id(id);
        when(podcastApi.createPodcastPost(request)).thenReturn(Mono.just(response));

        assertThat(client.create(request)).isSameAs(response);
    }

    @Test
    void updatePassesThrough() {
        UpdatePostRequest request = new UpdatePostRequest();
        PostResponse response = new PostResponse().id(id);
        when(podcastApi.updatePodcastPost(id, request)).thenReturn(Mono.just(response));

        assertThat(client.update(id, request)).isSameAs(response);
    }

    @Test
    void deleteDelegatesAndBlocks() {
        when(podcastApi.deletePodcastPost(id)).thenReturn(Mono.empty());

        client.delete(id);
        // no exception means the underlying Mono was invoked and blocked on
    }

    @Test
    void importPostsPassesThrough() {
        ImportPostsRequest request = new ImportPostsRequest();
        ImportResult response = new ImportResult();
        when(podcastApi.importPodcastPosts(request)).thenReturn(Mono.just(response));

        assertThat(client.importPosts(request)).isSameAs(response);
    }

    @Test
    void triggerSyncPassesThrough() {
        SyncResultResponse response = new SyncResultResponse();
        when(podcastApi.syncPodcastFromYoutube()).thenReturn(Mono.just(response));

        assertThat(client.triggerSync()).isSameAs(response);
    }

    @Test
    void getCategoriesPassesThroughTheWholeList() {
        List<CategoryResponse> categories = List.of(new CategoryResponse().slug("skate-clips"));
        when(podcastApi.getCategories()).thenReturn(Flux.fromIterable(categories));

        assertThat(client.getCategories()).containsExactlyElementsOf(categories);
    }

    @Test
    void getCategoryPostsPassesThrough() {
        FeedPageResponse response = new FeedPageResponse().page(0).size(10);
        when(podcastApi.getCategoryPosts("skate-clips", 0, 10)).thenReturn(Mono.just(response));

        assertThat(client.getCategoryPosts("skate-clips", 0, 10)).isSameAs(response);
    }

    @Test
    void getAdminCategoriesPassesThroughTheWholeList() {
        List<AdminCategoryResponse> categories = List.of(new AdminCategoryResponse().slug("skate-clips"));
        when(podcastApi.getAdminCategories()).thenReturn(Flux.fromIterable(categories));

        assertThat(client.getAdminCategories()).containsExactlyElementsOf(categories);
    }

    @Test
    void updateCategoryPassesThrough() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        AdminCategoryResponse response = new AdminCategoryResponse().slug("skate-clips");
        when(podcastApi.updateCategory(id, request)).thenReturn(Mono.just(response));

        assertThat(client.updateCategory(id, request)).isSameAs(response);
    }

    @Test
    void reorderCategoriesPassesThroughTheWholeList() {
        ReorderCategoriesRequest request = new ReorderCategoriesRequest();
        List<AdminCategoryResponse> categories = List.of(new AdminCategoryResponse().slug("skate-clips"));
        when(podcastApi.reorderCategories(request)).thenReturn(Flux.fromIterable(categories));

        assertThat(client.reorderCategories(request)).containsExactlyElementsOf(categories);
    }

    @Test
    void setDefaultCategoryPassesThrough() {
        AdminCategoryResponse response = new AdminCategoryResponse().slug("skate-clips");
        when(podcastApi.setDefaultCategory(id)).thenReturn(Mono.just(response));

        assertThat(client.setDefaultCategory(id)).isSameAs(response);
    }

    @Test
    void maps404ToNotFound() {
        when(podcastApi.getPodcastPostById(id)).thenReturn(Mono.error(responseException(HttpStatus.NOT_FOUND)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getById(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("PODCAST_NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("Podcast post not found");
    }

    @Test
    void maps400ToBadRequest() {
        when(podcastApi.createPodcastPost(any())).thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.create(new CreatePostRequest()), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("PODCAST_BAD_REQUEST");
        assertThat(ex.getMessage()).isEqualTo("Invalid podcast request");
    }

    @Test
    void mapsAnUnrecognizedStatusToTheGenericRequestErrorCode() {
        when(podcastApi.getPodcastPostById(id)).thenReturn(Mono.error(responseException(HttpStatus.CONFLICT)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getById(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getCode()).isEqualTo("PODCAST_REQUEST_ERROR");
        assertThat(ex.getMessage()).isEqualTo("Podcast service rejected the request");
    }

    @Test
    void maps5xxToServiceUnavailable() {
        when(podcastApi.getPodcastPostById(id)).thenReturn(Mono.error(responseException(HttpStatus.BAD_GATEWAY)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getById(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getCode()).isEqualTo("PODCAST_SERVICE_UNAVAILABLE");
    }

    @Test
    void mapsAConnectivityFailureOnAListCallToServiceUnavailable() {
        when(podcastApi.getCategories()).thenReturn(Flux.error(new WebClientRequestException(
                new RuntimeException("connection refused"), HttpMethod.GET,
                URI.create("http://podcast-be/api/categories"), HttpHeaders.EMPTY)));

        assertThatThrownBy(() -> client.getCategories())
                .isInstanceOf(DownstreamServiceException.class)
                .satisfies(t -> assertThat(((DownstreamServiceException) t).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void mapsAConnectivityFailureOnASingleCallToServiceUnavailable() {
        when(podcastApi.getPodcastPostById(id)).thenReturn(Mono.error(new WebClientRequestException(
                new RuntimeException("connection refused"), HttpMethod.GET,
                URI.create("http://podcast-be/api/podcast/" + id), HttpHeaders.EMPTY)));

        assertThatThrownBy(() -> client.getById(id))
                .isInstanceOf(DownstreamServiceException.class)
                .satisfies(t -> assertThat(((DownstreamServiceException) t).getCode())
                        .isEqualTo("PODCAST_SERVICE_UNAVAILABLE"));
    }
}
