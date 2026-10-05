package com.skateboard.uibackend.client.podcast;

import com.skateboard.uibackend.client.podcast.generated.api.GuestApplicationsApi;
import com.skateboard.uibackend.client.podcast.generated.model.CreateGuestApplicationRequest;
import com.skateboard.uibackend.client.podcast.generated.model.GuestApplicationPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.GuestApplicationResponse;
import com.skateboard.uibackend.client.podcast.generated.model.UpdateGuestApplicationStatusRequest;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wraps the generated {@link GuestApplicationsApi}, mirroring {@link
 * PodcastClient}'s blocking-call and exception-mapping shape. Kept separate
 * from {@link PodcastClient} despite sharing a downstream service and
 * {@code ApiClient} — same per-feature split {@link
 * com.skateboard.uibackend.client.appconfig.CampaignClient} uses relative to
 * {@link com.skateboard.uibackend.client.appconfig.AppConfigClient}.
 */
@Component
public class GuestApplicationClient {

    private final GuestApplicationsApi guestApplicationsApi;

    public GuestApplicationClient(GuestApplicationsApi guestApplicationsApi) {
        this.guestApplicationsApi = guestApplicationsApi;
    }

    public GuestApplicationResponse submit(CreateGuestApplicationRequest request) {
        return call(() -> guestApplicationsApi.submitGuestApplication(request), GuestApplicationClient::submitMessageFor);
    }

    public GuestApplicationResponse getMine() {
        return call(guestApplicationsApi::getMyGuestApplication);
    }

    public GuestApplicationPageResponse getAll(String status, Integer page, Integer size) {
        return call(() -> guestApplicationsApi.getGuestApplications(status, page, size));
    }

    public GuestApplicationResponse getById(UUID id) {
        return call(() -> guestApplicationsApi.getGuestApplicationById(id));
    }

    public GuestApplicationResponse updateStatus(UUID id, UpdateGuestApplicationStatusRequest request) {
        return call(() -> guestApplicationsApi.updateGuestApplicationStatus(id, request));
    }

    private <T> T call(Supplier<Mono<T>> invocation) {
        return call(invocation, GuestApplicationClient::messageFor);
    }

    private <T> T call(Supplier<Mono<T>> invocation, Function<HttpStatusCode, String> messageResolver) {
        try {
            return invocation.get().block();
        } catch (WebClientResponseException ex) {
            throw mapResponseException(ex, messageResolver);
        } catch (WebClientRequestException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private DownstreamServiceException mapResponseException(WebClientResponseException ex,
                                                              Function<HttpStatusCode, String> messageResolver) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.is5xxServerError()) {
            return serviceUnavailable(ex);
        }
        return new DownstreamServiceException(status, codeFor(status), messageResolver.apply(status), ex);
    }

    private DownstreamServiceException serviceUnavailable(Throwable cause) {
        return new DownstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "PODCAST_SERVICE_UNAVAILABLE",
                "Podcast service is currently unavailable", cause);
    }

    private static String codeFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "GUEST_APPLICATION_NOT_FOUND";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "GUEST_APPLICATION_BAD_REQUEST";
        }
        if (status.equals(HttpStatus.CONFLICT)) {
            return "GUEST_APPLICATION_CONFLICT";
        }
        return "GUEST_APPLICATION_REQUEST_ERROR";
    }

    private static String messageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "Guest application not found";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Invalid guest application request";
        }
        return "Podcast service rejected the request";
    }

    // Submission's only reachable error statuses are 400 (invalid input) and
    // 409 (the user already has an active application) — everything else
    // falls back to messageFor's generic text.
    private static String submitMessageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.CONFLICT)) {
            return "You already have an active guest application";
        }
        return messageFor(status);
    }
}
