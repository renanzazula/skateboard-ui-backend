package com.skateboard.uibackend.client.notification;

import com.skateboard.uibackend.client.notification.generated.api.DevicesApi;
import com.skateboard.uibackend.client.notification.generated.api.InboxApi;
import com.skateboard.uibackend.client.notification.generated.api.PreferencesApi;
import com.skateboard.uibackend.client.notification.generated.model.DeviceResponse;
import com.skateboard.uibackend.client.notification.generated.model.InboxPageResponse;
import com.skateboard.uibackend.client.notification.generated.model.MarkAllReadRequest;
import com.skateboard.uibackend.client.notification.generated.model.UnreadCountResponse;
import com.skateboard.uibackend.client.notification.generated.model.NotificationPreferencesResponse;
import com.skateboard.uibackend.client.notification.generated.model.TestNotificationResponse;
import com.skateboard.uibackend.client.notification.generated.model.RegisterDeviceRequest;
import com.skateboard.uibackend.client.notification.generated.model.UpdateNotificationPreferencesRequest;
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
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Every {@link NotificationClient} passthrough method and its shared
 * downstream-error mapping (mirrors {@link
 * com.skateboard.uibackend.client.appconfig.CampaignClientTest}'s shape).
 */
class NotificationClientTest {

    @Mock
    private DevicesApi devicesApi;

    @Mock
    private PreferencesApi preferencesApi;

    @Mock
    private InboxApi inboxApi;

    private NotificationClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new NotificationClient(devicesApi, preferencesApi, inboxApi);
    }

    private static WebClientResponseException responseException(HttpStatus status) {
        return WebClientResponseException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8, null);
    }

    @Test
    void registerDevicePassesThrough() {
        RegisterDeviceRequest request = new RegisterDeviceRequest();
        DeviceResponse response = new DeviceResponse();
        when(devicesApi.registerDevice("device-1", request)).thenReturn(Mono.just(response));

        assertThat(client.registerDevice("device-1", request)).isSameAs(response);
    }

    @Test
    void removeDeviceDelegatesAndBlocks() {
        when(devicesApi.removeDevice("device-1")).thenReturn(Mono.empty());

        client.removeDevice("device-1");
    }

    @Test
    void sendTestNotificationPassesThrough() {
        TestNotificationResponse response = new TestNotificationResponse();
        when(devicesApi.sendTestNotification()).thenReturn(Mono.just(response));

        assertThat(client.sendTestNotification()).isSameAs(response);
    }

    @Test
    void getNotificationPreferencesPassesThrough() {
        NotificationPreferencesResponse response = new NotificationPreferencesResponse();
        when(preferencesApi.getNotificationPreferences()).thenReturn(Mono.just(response));

        assertThat(client.getNotificationPreferences()).isSameAs(response);
    }

    @Test
    void updateNotificationPreferencesPassesThrough() {
        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest();
        NotificationPreferencesResponse response = new NotificationPreferencesResponse();
        when(preferencesApi.updateNotificationPreferences(request)).thenReturn(Mono.just(response));

        assertThat(client.updateNotificationPreferences(request)).isSameAs(response);
    }

    @Test
    void maps404ToNotFound() {
        when(preferencesApi.getNotificationPreferences()).thenReturn(Mono.error(responseException(HttpStatus.NOT_FOUND)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.getNotificationPreferences(), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("NOTIFICATION_NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("Notification resource not found");
    }

    @Test
    void maps400ToBadRequest() {
        RegisterDeviceRequest request = new RegisterDeviceRequest();
        when(devicesApi.registerDevice("device-1", request)).thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.registerDevice("device-1", request), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("NOTIFICATION_BAD_REQUEST");
        assertThat(ex.getMessage()).isEqualTo("Invalid notification request");
    }

    @Test
    void mapsAnUnrecognizedStatusToTheGenericRequestErrorCode() {
        when(preferencesApi.getNotificationPreferences()).thenReturn(Mono.error(responseException(HttpStatus.CONFLICT)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.getNotificationPreferences(), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getCode()).isEqualTo("NOTIFICATION_REQUEST_ERROR");
        assertThat(ex.getMessage()).isEqualTo("Notification service rejected the request");
    }

    @Test
    void maps5xxToServiceUnavailable() {
        when(preferencesApi.getNotificationPreferences()).thenReturn(Mono.error(responseException(HttpStatus.BAD_GATEWAY)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.getNotificationPreferences(), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getCode()).isEqualTo("NOTIFICATION_SERVICE_UNAVAILABLE");
    }

    @Test
    void mapsAConnectivityFailureToServiceUnavailable() {
        when(preferencesApi.getNotificationPreferences()).thenReturn(Mono.error(new WebClientRequestException(
                new RuntimeException("connection refused"), HttpMethod.GET,
                URI.create("http://notification-be/api/preferences"), HttpHeaders.EMPTY)));

        assertThatThrownBy(() -> client.getNotificationPreferences())
                .isInstanceOf(DownstreamServiceException.class)
                .satisfies(t -> assertThat(((DownstreamServiceException) t).getCode())
                        .isEqualTo("NOTIFICATION_SERVICE_UNAVAILABLE"));
    }

    @Test
    void listInboxPassesThrough() {
        InboxPageResponse response = new InboxPageResponse();
        when(inboxApi.listInbox(1, 20)).thenReturn(Mono.just(response));

        assertThat(client.listInbox(1, 20)).isSameAs(response);
    }

    @Test
    void getInboxUnreadCountPassesThrough() {
        UnreadCountResponse response = new UnreadCountResponse().count(6L);
        when(inboxApi.getInboxUnreadCount()).thenReturn(Mono.just(response));

        assertThat(client.getInboxUnreadCount()).isSameAs(response);
    }

    @Test
    void markInboxNotificationReadMapsANotFound() {
        UUID id = UUID.randomUUID();
        when(inboxApi.markInboxNotificationRead(id)).thenReturn(Mono.error(responseException(HttpStatus.NOT_FOUND)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.markInboxNotificationRead(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("NOTIFICATION_NOT_FOUND");
    }

    /** The downstream route consumes JSON only, so a missing body must still be sent as {}. */
    @Test
    void markAllInboxNotificationsReadAlwaysSendsABody() {
        when(inboxApi.markAllInboxNotificationsRead(any())).thenReturn(Mono.empty());

        client.markAllInboxNotificationsRead(null);

        verify(inboxApi).markAllInboxNotificationsRead(notNull());
    }

    @Test
    void markAllInboxNotificationsReadPassesTheCutOffThrough() {
        MarkAllReadRequest request = new MarkAllReadRequest();
        when(inboxApi.markAllInboxNotificationsRead(request)).thenReturn(Mono.empty());

        client.markAllInboxNotificationsRead(request);

        verify(inboxApi).markAllInboxNotificationsRead(request);
    }
}
