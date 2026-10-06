package com.skateboard.uibackend.client.podcast;

import com.skateboard.uibackend.client.podcast.generated.api.GuestApplicationsApi;
import com.skateboard.uibackend.client.podcast.generated.model.CreateGuestApplicationRequest;
import com.skateboard.uibackend.client.podcast.generated.model.GuestApplicationPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.GuestApplicationResponse;
import com.skateboard.uibackend.client.podcast.generated.model.UpdateGuestApplicationStatusRequest;
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
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Every {@link GuestApplicationClient} passthrough method, mirroring {@link PodcastClientTest}'s shape. */
class GuestApplicationClientTest {

    @Mock
    private GuestApplicationsApi guestApplicationsApi;

    private GuestApplicationClient client;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new GuestApplicationClient(guestApplicationsApi);
    }

    private static WebClientResponseException responseException(HttpStatus status) {
        return WebClientResponseException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8, null);
    }

    @Test
    void submitPassesThrough() {
        CreateGuestApplicationRequest request = new CreateGuestApplicationRequest();
        GuestApplicationResponse response = new GuestApplicationResponse().id(id);
        when(guestApplicationsApi.submitGuestApplication(request)).thenReturn(Mono.just(response));

        assertThat(client.submit(request)).isSameAs(response);
    }

    @Test
    void getMinePassesThrough() {
        GuestApplicationResponse response = new GuestApplicationResponse().id(id);
        when(guestApplicationsApi.getMyGuestApplication()).thenReturn(Mono.just(response));

        assertThat(client.getMine()).isSameAs(response);
    }

    @Test
    void getAllPassesThrough() {
        GuestApplicationPageResponse response = new GuestApplicationPageResponse().page(0).size(10);
        when(guestApplicationsApi.getGuestApplications("NEW", 0, 10)).thenReturn(Mono.just(response));

        assertThat(client.getAll("NEW", 0, 10)).isSameAs(response);
    }

    @Test
    void getByIdPassesThrough() {
        GuestApplicationResponse response = new GuestApplicationResponse().id(id);
        when(guestApplicationsApi.getGuestApplicationById(id)).thenReturn(Mono.just(response));

        assertThat(client.getById(id)).isSameAs(response);
    }

    @Test
    void updateStatusPassesThrough() {
        UpdateGuestApplicationStatusRequest request =
                new UpdateGuestApplicationStatusRequest().status(UpdateGuestApplicationStatusRequest.StatusEnum.ACCEPTED);
        GuestApplicationResponse response = new GuestApplicationResponse().id(id);
        when(guestApplicationsApi.updateGuestApplicationStatus(id, request)).thenReturn(Mono.just(response));

        assertThat(client.updateStatus(id, request)).isSameAs(response);
    }

    @Test
    void mapsAConflictOnSubmitToADedicatedMessage() {
        when(guestApplicationsApi.submitGuestApplication(any()))
                .thenReturn(Mono.error(responseException(HttpStatus.CONFLICT)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.submit(new CreateGuestApplicationRequest()), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getCode()).isEqualTo("GUEST_APPLICATION_CONFLICT");
        assertThat(ex.getMessage()).contains("already have an active guest application");
    }

    @Test
    void maps404ToNotFound() {
        when(guestApplicationsApi.getGuestApplicationById(id)).thenReturn(Mono.error(responseException(HttpStatus.NOT_FOUND)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getById(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("GUEST_APPLICATION_NOT_FOUND");
    }

    @Test
    void mapsA5xxToServiceUnavailable() {
        when(guestApplicationsApi.getMyGuestApplication())
                .thenReturn(Mono.error(responseException(HttpStatus.INTERNAL_SERVER_ERROR)));

        DownstreamServiceException ex = catchThrowableOfType(client::getMine, DownstreamServiceException.class);

        assertThat(ex.getCode()).isEqualTo("PODCAST_SERVICE_UNAVAILABLE");
    }

    @Test
    void mapsAConnectivityFailureToServiceUnavailable() {
        when(guestApplicationsApi.getMyGuestApplication())
                .thenReturn(Mono.error(new WebClientRequestException(
                        new RuntimeException("connection refused"), HttpMethod.GET,
                        URI.create("http://podcast-be/api/guest-applications/me"), HttpHeaders.EMPTY)));

        DownstreamServiceException ex = catchThrowableOfType(client::getMine, DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getCode()).isEqualTo("PODCAST_SERVICE_UNAVAILABLE");
    }
}
