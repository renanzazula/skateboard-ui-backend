package com.skateboard.uibackend.client.podcast;

import com.skateboard.uibackend.client.podcast.generated.api.PodcastApi;
import com.skateboard.uibackend.client.podcast.generated.model.CategoryResponse;
import com.skateboard.uibackend.client.podcast.generated.model.CreatePostRequest;
import com.skateboard.uibackend.client.podcast.generated.model.FeedPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.ImportPostsRequest;
import com.skateboard.uibackend.client.podcast.generated.model.ImportResult;
import com.skateboard.uibackend.client.podcast.generated.model.PostResponse;
import com.skateboard.uibackend.client.podcast.generated.model.SyncResultResponse;
import com.skateboard.uibackend.client.podcast.generated.model.UpdatePostRequest;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Wraps the generated {@link PodcastApi}: blocks on each call (this BFF is a
 * classic servlet MVC app), and translates WebClient failures into
 * {@link DownstreamServiceException} rather than letting reactive/HTTP
 * exception types leak into controllers. A downstream 5xx or a connectivity
 * failure (timeout, connection refused) both become
 * {@code PODCAST_SERVICE_UNAVAILABLE} — genuinely an outage from the
 * frontend's perspective. A downstream 4xx is passed through with the same
 * status: that's the downstream service's own validation/not-found result,
 * not something to hide.
 */
@Component
public class PodcastClient {

    private final PodcastApi podcastApi;

    public PodcastClient(PodcastApi podcastApi) {
        this.podcastApi = podcastApi;
    }

    public FeedPageResponse getFeed(Integer page, Integer size) {
        return call(() -> podcastApi.getPodcastFeed(page, size));
    }

    public PostResponse getBySlug(String slug) {
        return call(() -> podcastApi.getPodcastPostBySlug(slug));
    }

    public PostResponse create(CreatePostRequest request) {
        return call(() -> podcastApi.createPodcastPost(request));
    }

    public PostResponse update(UUID id, UpdatePostRequest request) {
        return call(() -> podcastApi.updatePodcastPost(id, request));
    }

    public void delete(UUID id) {
        call(() -> podcastApi.deletePodcastPost(id));
    }

    public ImportResult importPosts(ImportPostsRequest request) {
        return call(() -> podcastApi.importPodcastPosts(request));
    }

    public SyncResultResponse triggerSync() {
        return call(podcastApi::syncPodcastFromYoutube);
    }

    public List<CategoryResponse> getCategories() {
        return callList(podcastApi::getCategories);
    }

    public FeedPageResponse getCategoryPosts(String slug, Integer page, Integer size) {
        return call(() -> podcastApi.getCategoryPosts(slug, page, size));
    }

    private <T> T call(Supplier<Mono<T>> invocation) {
        try {
            return invocation.get().block();
        } catch (WebClientResponseException ex) {
            throw mapResponseException(ex);
        } catch (WebClientRequestException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private <T> List<T> callList(Supplier<Flux<T>> invocation) {
        try {
            return invocation.get().collectList().block();
        } catch (WebClientResponseException ex) {
            throw mapResponseException(ex);
        } catch (WebClientRequestException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private DownstreamServiceException mapResponseException(WebClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.is5xxServerError()) {
            return serviceUnavailable(ex);
        }
        return new DownstreamServiceException(status, codeFor(status), messageFor(status), ex);
    }

    private DownstreamServiceException serviceUnavailable(Throwable cause) {
        return new DownstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "PODCAST_SERVICE_UNAVAILABLE",
                "Podcast service is currently unavailable", cause);
    }

    private static String codeFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "PODCAST_NOT_FOUND";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "PODCAST_BAD_REQUEST";
        }
        return "PODCAST_REQUEST_ERROR";
    }

    private static String messageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "Podcast post not found";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Invalid podcast request";
        }
        return "Podcast service rejected the request";
    }
}
