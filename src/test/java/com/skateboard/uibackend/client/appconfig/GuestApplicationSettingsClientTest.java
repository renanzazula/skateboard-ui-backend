package com.skateboard.uibackend.client.appconfig;

import com.skateboard.uibackend.client.appconfig.generated.api.GuestApplicationSettingsApi;
import com.skateboard.uibackend.client.appconfig.generated.model.GuestApplicationSettingsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicGuestApplicationSettingsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateGuestApplicationSettingsRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

class GuestApplicationSettingsClientTest {

    @Mock
    private GuestApplicationSettingsApi guestApplicationSettingsApi;

    private GuestApplicationSettingsClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new GuestApplicationSettingsClient(guestApplicationSettingsApi);
    }

    private static WebClientResponseException responseException(HttpStatus status) {
        return WebClientResponseException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8, null);
    }

    @Test
    void getPublicPassesThrough() {
        PublicGuestApplicationSettingsResponse response = new PublicGuestApplicationSettingsResponse().enabled(true);
        when(guestApplicationSettingsApi.getGuestApplicationSettingsPublic()).thenReturn(Mono.just(response));

        assertThat(client.getPublic()).isSameAs(response);
    }

    @Test
    void getAdminPassesThrough() {
        GuestApplicationSettingsResponse response = new GuestApplicationSettingsResponse().enabled(true);
        when(guestApplicationSettingsApi.getGuestApplicationSettingsAdmin()).thenReturn(Mono.just(response));

        assertThat(client.getAdmin()).isSameAs(response);
    }

    @Test
    void updatePassesThrough() {
        UpdateGuestApplicationSettingsRequest request = new UpdateGuestApplicationSettingsRequest();
        GuestApplicationSettingsResponse response = new GuestApplicationSettingsResponse().enabled(true);
        when(guestApplicationSettingsApi.updateGuestApplicationSettings(request)).thenReturn(Mono.just(response));

        assertThat(client.update(request)).isSameAs(response);
    }

    @Test
    void mapsABadRequestOnUpdateToADedicatedMessage() {
        UpdateGuestApplicationSettingsRequest request = new UpdateGuestApplicationSettingsRequest();
        when(guestApplicationSettingsApi.updateGuestApplicationSettings(request))
                .thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST)));

        DownstreamServiceException ex =
                catchThrowableOfType(() -> client.update(request), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_BAD_REQUEST");
        assertThat(ex.getMessage()).contains("at least one recipient");
    }

    @Test
    void mapsA5xxToServiceUnavailable() {
        when(guestApplicationSettingsApi.getGuestApplicationSettingsAdmin())
                .thenReturn(Mono.error(responseException(HttpStatus.INTERNAL_SERVER_ERROR)));

        DownstreamServiceException ex = catchThrowableOfType(client::getAdmin, DownstreamServiceException.class);

        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_SERVICE_UNAVAILABLE");
    }

    @Test
    void mapsAConnectivityFailureToServiceUnavailable() {
        when(guestApplicationSettingsApi.getGuestApplicationSettingsAdmin())
                .thenReturn(Mono.error(new WebClientRequestException(
                        new RuntimeException("connection refused"), HttpMethod.GET,
                        URI.create("http://app-config-be/api/guest-application-settings/admin"), HttpHeaders.EMPTY)));

        DownstreamServiceException ex = catchThrowableOfType(client::getAdmin, DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
