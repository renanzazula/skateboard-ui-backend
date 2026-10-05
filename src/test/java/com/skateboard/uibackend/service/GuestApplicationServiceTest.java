package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.GuestApplicationSettingsClient;
import com.skateboard.uibackend.client.appconfig.generated.model.GuestApplicationSettingsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicGuestApplicationSettingsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateGuestApplicationSettingsRequest;
import com.skateboard.uibackend.client.podcast.GuestApplicationClient;
import com.skateboard.uibackend.client.podcast.generated.model.CreateGuestApplicationRequest;
import com.skateboard.uibackend.client.podcast.generated.model.GuestApplicationPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.GuestApplicationResponse;
import com.skateboard.uibackend.client.podcast.generated.model.UpdateGuestApplicationStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GuestApplicationService} is a thin passthrough composing two
 * downstream clients — mirrors {@link PodcastServiceTest}'s shape.
 */
class GuestApplicationServiceTest {

    @Mock private GuestApplicationClient guestApplicationClient;
    @Mock private GuestApplicationSettingsClient guestApplicationSettingsClient;

    private GuestApplicationService service;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GuestApplicationService(guestApplicationClient, guestApplicationSettingsClient);
    }

    @Test
    void submitDelegatesToThePodcastClient() {
        CreateGuestApplicationRequest request = new CreateGuestApplicationRequest();
        GuestApplicationResponse response = new GuestApplicationResponse().id(id);
        when(guestApplicationClient.submit(request)).thenReturn(response);

        assertThat(service.submit(request)).isSameAs(response);
    }

    @Test
    void getMineDelegates() {
        GuestApplicationResponse response = new GuestApplicationResponse().id(id);
        when(guestApplicationClient.getMine()).thenReturn(response);

        assertThat(service.getMine()).isSameAs(response);
    }

    @Test
    void getAllDelegates() {
        GuestApplicationPageResponse response = new GuestApplicationPageResponse();
        when(guestApplicationClient.getAll("NEW", 0, 10)).thenReturn(response);

        assertThat(service.getAll("NEW", 0, 10)).isSameAs(response);
    }

    @Test
    void getByIdDelegates() {
        GuestApplicationResponse response = new GuestApplicationResponse().id(id);
        when(guestApplicationClient.getById(id)).thenReturn(response);

        assertThat(service.getById(id)).isSameAs(response);
    }

    @Test
    void updateStatusDelegates() {
        UpdateGuestApplicationStatusRequest request =
                new UpdateGuestApplicationStatusRequest().status(UpdateGuestApplicationStatusRequest.StatusEnum.ACCEPTED);
        GuestApplicationResponse response = new GuestApplicationResponse().id(id);
        when(guestApplicationClient.updateStatus(id, request)).thenReturn(response);

        assertThat(service.updateStatus(id, request)).isSameAs(response);
    }

    @Test
    void getPublicSettingsDelegatesToTheAppConfigClient() {
        PublicGuestApplicationSettingsResponse response = new PublicGuestApplicationSettingsResponse().enabled(true);
        when(guestApplicationSettingsClient.getPublic()).thenReturn(response);

        assertThat(service.getPublicSettings()).isSameAs(response);
    }

    @Test
    void getAdminSettingsDelegates() {
        GuestApplicationSettingsResponse response = new GuestApplicationSettingsResponse().enabled(true);
        when(guestApplicationSettingsClient.getAdmin()).thenReturn(response);

        assertThat(service.getAdminSettings()).isSameAs(response);
    }

    @Test
    void updateSettingsDelegates() {
        UpdateGuestApplicationSettingsRequest request = new UpdateGuestApplicationSettingsRequest();
        GuestApplicationSettingsResponse response = new GuestApplicationSettingsResponse().enabled(true);
        when(guestApplicationSettingsClient.update(request)).thenReturn(response);

        assertThat(service.updateSettings(request)).isSameAs(response);
        verify(guestApplicationSettingsClient).update(request);
    }
}
