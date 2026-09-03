package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.notification.generated.model.DeviceResponse;
import com.skateboard.uibackend.client.notification.generated.model.NotificationPreferencesResponse;
import com.skateboard.uibackend.client.notification.generated.model.RegisterDeviceRequest;
import com.skateboard.uibackend.client.notification.generated.model.UpdateNotificationPreferencesRequest;
import com.skateboard.uibackend.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes skateboard-notification-be under {@code /api/me/**}, the BFF-facing
 * prefix every other endpoint here uses, even though that service's own paths
 * are {@code /devices} and {@code /preferences}. The {@code @PreAuthorize}
 * authorities are copied from api/notification-openapi.yaml's
 * {@code x-required-permissions} — coarse-grained, token-claim-only checks at
 * this API boundary, not a duplicate of the downstream service's own
 * authorization, which still runs against the relayed token.
 *
 * <p>{@code /api/me/preferences} used to be served by
 * {@link UserController} against skateboard-user-be, which owned notification
 * preferences before this service existed. The route, the request and response
 * shapes and the required authorities are all unchanged — only what stands
 * behind them moved — so the mobile settings screen needs no change. Keeping
 * these two methods next to device registration reflects who actually serves
 * them; leaving them on UserController would say skateboard-user-be does.
 */
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PutMapping("/api/me/devices/{deviceIdentifier}")
    @PreAuthorize("hasAuthority('FUNC_NOTIFICATION_DEVICE_MANAGE')")
    public DeviceResponse registerDevice(@PathVariable String deviceIdentifier,
                                          @RequestBody RegisterDeviceRequest request) {
        return notificationService.registerDevice(deviceIdentifier, request);
    }

    @DeleteMapping("/api/me/devices/{deviceIdentifier}")
    @PreAuthorize("hasAuthority('FUNC_NOTIFICATION_DEVICE_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDevice(@PathVariable String deviceIdentifier) {
        notificationService.removeDevice(deviceIdentifier);
    }

    @GetMapping("/api/me/preferences")
    @PreAuthorize("hasAuthority('FUNC_USER_SELF_READ')")
    public NotificationPreferencesResponse getNotificationPreferences() {
        return notificationService.getNotificationPreferences();
    }

    @PatchMapping("/api/me/preferences")
    @PreAuthorize("hasAuthority('FUNC_USER_SELF_UPDATE')")
    public NotificationPreferencesResponse updateNotificationPreferences(
            @RequestBody UpdateNotificationPreferencesRequest request) {
        return notificationService.updateNotificationPreferences(request);
    }
}
