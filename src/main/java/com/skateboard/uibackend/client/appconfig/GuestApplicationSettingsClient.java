package com.skateboard.uibackend.client.appconfig;

import com.skateboard.uibackend.client.appconfig.generated.api.GuestApplicationSettingsApi;
import com.skateboard.uibackend.client.appconfig.generated.model.GuestApplicationSettingsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicGuestApplicationSettingsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateGuestApplicationSettingsRequest;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Wraps the generated {@link GuestApplicationSettingsApi}, mirroring {@link
 * AppConfigClient}'s blocking-call and exception-mapping shape. Kept separate
 * from {@link AppConfigClient} for the same per-feature reason {@link
 * CampaignClient} is.
 */
@Component
public class GuestApplicationSettingsClient {

    private final GuestApplicationSettingsApi guestApplicationSettingsApi;

    public GuestApplicationSettingsClient(GuestApplicationSettingsApi guestApplicationSettingsApi) {
        this.guestApplicationSettingsApi = guestApplicationSettingsApi;
    }

    public PublicGuestApplicationSettingsResponse getPublic() {
        return call(guestApplicationSettingsApi::getGuestApplicationSettingsPublic);
    }

    public GuestApplicationSettingsResponse getAdmin() {
        return call(guestApplicationSettingsApi::getGuestApplicationSettingsAdmin);
    }

    public GuestApplicationSettingsResponse update(UpdateGuestApplicationSettingsRequest request) {
        return call(() -> guestApplicationSettingsApi.updateGuestApplicationSettings(request));
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

    private DownstreamServiceException mapResponseException(WebClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.is5xxServerError()) {
            return serviceUnavailable(ex);
        }
        return new DownstreamServiceException(status, codeFor(status), messageFor(status), ex);
    }

    private DownstreamServiceException serviceUnavailable(Throwable cause) {
        return new DownstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "APP_CONFIG_SERVICE_UNAVAILABLE",
                "App config service is currently unavailable", cause);
    }

    private static String codeFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "APP_CONFIG_BAD_REQUEST";
        }
        return "APP_CONFIG_REQUEST_ERROR";
    }

    // The only reachable error status for this feature's update is 400
    // (enabling with no recipients). Confirmation/admin-notification copy
    // validation now lives on the email-templates endpoints, not here.
    private static String messageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Select at least one recipient to enable submissions.";
        }
        return "App config service rejected the request";
    }
}
