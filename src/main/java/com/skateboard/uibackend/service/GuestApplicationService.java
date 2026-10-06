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
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Thin pass-through, same seam as {@link PodcastService}/{@link
 * AppConfigService} — composes the two downstream clients this one feature
 * area spans (applications live in skateboard-podcast-be, settings in
 * skateboard-app-config-be) rather than forcing the controller to know
 * about both.
 */
@Service
public class GuestApplicationService {

    private final GuestApplicationClient guestApplicationClient;
    private final GuestApplicationSettingsClient guestApplicationSettingsClient;

    public GuestApplicationService(GuestApplicationClient guestApplicationClient,
                                   GuestApplicationSettingsClient guestApplicationSettingsClient) {
        this.guestApplicationClient = guestApplicationClient;
        this.guestApplicationSettingsClient = guestApplicationSettingsClient;
    }

    public GuestApplicationResponse submit(CreateGuestApplicationRequest request) {
        return guestApplicationClient.submit(request);
    }

    public GuestApplicationResponse getMine() {
        return guestApplicationClient.getMine();
    }

    public GuestApplicationPageResponse getAll(String status, Integer page, Integer size) {
        return guestApplicationClient.getAll(status, page, size);
    }

    public GuestApplicationResponse getById(UUID id) {
        return guestApplicationClient.getById(id);
    }

    public GuestApplicationResponse updateStatus(UUID id, UpdateGuestApplicationStatusRequest request) {
        return guestApplicationClient.updateStatus(id, request);
    }

    public PublicGuestApplicationSettingsResponse getPublicSettings() {
        return guestApplicationSettingsClient.getPublic();
    }

    public GuestApplicationSettingsResponse getAdminSettings() {
        return guestApplicationSettingsClient.getAdmin();
    }

    public GuestApplicationSettingsResponse updateSettings(UpdateGuestApplicationSettingsRequest request) {
        return guestApplicationSettingsClient.update(request);
    }
}
