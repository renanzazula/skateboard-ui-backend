package com.skateboard.uibackend.client.notification;

import com.skateboard.uibackend.client.notification.generated.api.DevicesApi;
import com.skateboard.uibackend.client.notification.generated.api.PreferencesApi;
import com.skateboard.uibackend.client.notification.generated.model.DeviceResponse;
import com.skateboard.uibackend.client.notification.generated.model.NotificationPreferencesResponse;
import com.skateboard.uibackend.client.notification.generated.model.RegisterDeviceRequest;
import com.skateboard.uibackend.client.notification.generated.model.UpdateNotificationPreferencesRequest;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Wraps the generated notification client: blocks on each call (this BFF is a
 * classic servlet MVC app) and translates WebClient failures into
 * {@link DownstreamServiceException} rather than letting reactive/HTTP
 * exception types leak into controllers. A downstream 5xx or a connectivity
 * failure both become {@code NOTIFICATION_SERVICE_UNAVAILABLE} — an outage
 * from the frontend's perspective. A downstream 4xx is passed through with its
 * original status: that is the downstream service's own validation result, not
 * something to hide.
 *
 * <p>Mirrors {@code PodcastClient}. Note what that means for device
 * registration: an outage here surfaces to the app as a 503, so the app must
 * treat registering a push token as retriable rather than as a step that has
 * to succeed before the user can continue.
 */
@Component
public class NotificationClient {

    private final DevicesApi devicesApi;
    private final PreferencesApi preferencesApi;

    public NotificationClient(DevicesApi notificationDevicesApi, PreferencesApi notificationPreferencesApi) {
        this.devicesApi = notificationDevicesApi;
        this.preferencesApi = notificationPreferencesApi;
    }

    public DeviceResponse registerDevice(String deviceIdentifier, RegisterDeviceRequest request) {
        return call(() -> devicesApi.registerDevice(deviceIdentifier, request));
    }

    public void removeDevice(String deviceIdentifier) {
        call(() -> devicesApi.removeDevice(deviceIdentifier));
    }

    public NotificationPreferencesResponse getNotificationPreferences() {
        return call(preferencesApi::getNotificationPreferences);
    }

    public NotificationPreferencesResponse updateNotificationPreferences(
            UpdateNotificationPreferencesRequest request) {
        return call(() -> preferencesApi.updateNotificationPreferences(request));
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
        return new DownstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "NOTIFICATION_SERVICE_UNAVAILABLE", "Notification service is currently unavailable", cause);
    }

    private static String codeFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND))   return "NOTIFICATION_NOT_FOUND";
        if (status.equals(HttpStatus.BAD_REQUEST)) return "NOTIFICATION_BAD_REQUEST";
        return "NOTIFICATION_REQUEST_ERROR";
    }

    private static String messageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND))   return "Notification resource not found";
        if (status.equals(HttpStatus.BAD_REQUEST)) return "Invalid notification request";
        return "Notification service rejected the request";
    }
}
