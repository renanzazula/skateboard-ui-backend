package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.notification.NotificationClient;
import com.skateboard.uibackend.client.notification.generated.model.DeviceResponse;
import com.skateboard.uibackend.client.notification.generated.model.NotificationPreferencesResponse;
import com.skateboard.uibackend.client.notification.generated.model.TestNotificationResponse;
import com.skateboard.uibackend.client.notification.generated.model.RegisterDeviceRequest;
import com.skateboard.uibackend.client.notification.generated.model.UpdateNotificationPreferencesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotificationService} is a thin passthrough to {@link
 * NotificationClient} — mirrors {@link AppConfigServiceTest}'s shape.
 */
class NotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NotificationService(notificationClient);
    }

    @Test
    void registerDeviceDelegates() {
        RegisterDeviceRequest request = new RegisterDeviceRequest();
        DeviceResponse response = new DeviceResponse();
        when(notificationClient.registerDevice("device-1", request)).thenReturn(response);

        assertThat(service.registerDevice("device-1", request)).isSameAs(response);
    }

    @Test
    void removeDeviceDelegates() {
        service.removeDevice("device-1");

        verify(notificationClient).removeDevice("device-1");
    }

    @Test
    void sendTestNotificationDelegates() {
        TestNotificationResponse response = new TestNotificationResponse();
        when(notificationClient.sendTestNotification()).thenReturn(response);

        assertThat(service.sendTestNotification()).isSameAs(response);
    }

    @Test
    void getNotificationPreferencesDelegates() {
        NotificationPreferencesResponse response = new NotificationPreferencesResponse();
        when(notificationClient.getNotificationPreferences()).thenReturn(response);

        assertThat(service.getNotificationPreferences()).isSameAs(response);
    }

    @Test
    void updateNotificationPreferencesDelegates() {
        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest();
        NotificationPreferencesResponse response = new NotificationPreferencesResponse();
        when(notificationClient.updateNotificationPreferences(request)).thenReturn(response);

        assertThat(service.updateNotificationPreferences(request)).isSameAs(response);
    }
}
