package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.notification.NotificationClient;
import com.skateboard.uibackend.client.notification.generated.model.DeviceResponse;
import com.skateboard.uibackend.client.notification.generated.model.NotificationPreferencesResponse;
import com.skateboard.uibackend.client.notification.generated.model.RegisterDeviceRequest;
import com.skateboard.uibackend.client.notification.generated.model.UpdateNotificationPreferencesRequest;
import org.springframework.stereotype.Service;

/**
 * Thin pass-through today — the seam where orchestration across downstream
 * clients belongs once the frontend needs it (a notification inbox that also
 * resolved podcast covers, say).
 */
@Service
public class NotificationService {

    private final NotificationClient notificationClient;

    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public DeviceResponse registerDevice(String deviceIdentifier, RegisterDeviceRequest request) {
        return notificationClient.registerDevice(deviceIdentifier, request);
    }

    public void removeDevice(String deviceIdentifier) {
        notificationClient.removeDevice(deviceIdentifier);
    }

    public NotificationPreferencesResponse getNotificationPreferences() {
        return notificationClient.getNotificationPreferences();
    }

    public NotificationPreferencesResponse updateNotificationPreferences(
            UpdateNotificationPreferencesRequest request) {
        return notificationClient.updateNotificationPreferences(request);
    }
}
