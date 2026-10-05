package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.GuestApplicationSettingsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicGuestApplicationSettingsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateGuestApplicationSettingsRequest;
import com.skateboard.uibackend.client.podcast.generated.model.CreateGuestApplicationRequest;
import com.skateboard.uibackend.client.podcast.generated.model.GuestApplicationPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.GuestApplicationResponse;
import com.skateboard.uibackend.client.podcast.generated.model.UpdateGuestApplicationStatusRequest;
import com.skateboard.uibackend.service.GuestApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Passes the frontend straight through to skateboard-podcast-be's
 * {@code /api/guest-applications/**} and skateboard-app-config-be's
 * {@code /api/guest-application-settings/**}, mirroring {@link
 * PodcastController}/{@link AppConfigController} — one controller for both,
 * since the frontend sees this as a single feature area (Settings →
 * Community → Be a Podcast Guest, and its admin screens), matching how
 * {@link AboutUsController} fronts app-config-be alone. The
 * {@code @PreAuthorize} authorities are copied from each vendored spec's
 * {@code x-required-permissions}; {@code GET /api/guest-application-settings}
 * carries none there (any authenticated caller), so it carries none here
 * either — {@code anyRequest().authenticated()} in {@code SecurityConfig}
 * already covers it.
 */
@RestController
public class GuestApplicationController {

    private final GuestApplicationService guestApplicationService;

    public GuestApplicationController(GuestApplicationService guestApplicationService) {
        this.guestApplicationService = guestApplicationService;
    }

    @PostMapping("/api/guest-applications")
    @PreAuthorize("hasAuthority('FUNC_GUEST_APPLICATION_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public GuestApplicationResponse submit(@RequestBody CreateGuestApplicationRequest request) {
        return guestApplicationService.submit(request);
    }

    @GetMapping("/api/guest-applications/me")
    @PreAuthorize("hasAuthority('FUNC_GUEST_APPLICATION_READ_OWN')")
    public GuestApplicationResponse getMine() {
        return guestApplicationService.getMine();
    }

    @GetMapping("/api/admin/guest-applications")
    @PreAuthorize("hasAuthority('FUNC_GUEST_APPLICATION_MANAGE')")
    public GuestApplicationPageResponse getAll(@RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "0") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size) {
        return guestApplicationService.getAll(status, page, size);
    }

    @GetMapping("/api/admin/guest-applications/{id}")
    @PreAuthorize("hasAuthority('FUNC_GUEST_APPLICATION_MANAGE')")
    public GuestApplicationResponse getById(@PathVariable UUID id) {
        return guestApplicationService.getById(id);
    }

    @PatchMapping("/api/admin/guest-applications/{id}/status")
    @PreAuthorize("hasAuthority('FUNC_GUEST_APPLICATION_MANAGE')")
    public GuestApplicationResponse updateStatus(@PathVariable UUID id,
                                                  @RequestBody UpdateGuestApplicationStatusRequest request) {
        return guestApplicationService.updateStatus(id, request);
    }

    @GetMapping("/api/guest-application-settings")
    public PublicGuestApplicationSettingsResponse getPublicSettings() {
        return guestApplicationService.getPublicSettings();
    }

    @GetMapping("/api/guest-application-settings/admin")
    @PreAuthorize("hasAuthority('FUNC_GUEST_APPLICATION_CONFIGURE')")
    public GuestApplicationSettingsResponse getAdminSettings() {
        return guestApplicationService.getAdminSettings();
    }

    @PutMapping("/api/guest-application-settings/admin")
    @PreAuthorize("hasAuthority('FUNC_GUEST_APPLICATION_CONFIGURE')")
    public GuestApplicationSettingsResponse updateSettings(@RequestBody UpdateGuestApplicationSettingsRequest request) {
        return guestApplicationService.updateSettings(request);
    }
}
