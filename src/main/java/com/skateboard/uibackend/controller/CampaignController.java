package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.CampaignEventRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRuntimeResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.ReorderCampaignScreensRequest;
import com.skateboard.uibackend.service.CampaignService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Passes the frontend straight through to skateboard-app-config-be's
 * {@code /api/campaigns/**} routes, mirroring {@link AppConfigController}.
 * <p>
 * {@code GET /api/campaigns/active} and {@code POST /api/campaigns/{id}/events}
 * are the two pre-auth routes — the vendored {@code campaign} tag carries no
 * {@code x-required-permissions} on them, and {@code SecurityConfig} permits
 * them so an ANONYMOUS/ALL campaign can resolve and report analytics before
 * login. Every {@code /api/campaigns/admin/**} route mirrors its own spec
 * entry's {@code x-required-permissions}: {@code FUNC_CAMPAIGN_READ} for the
 * reads, {@code FUNC_CAMPAIGN_MANAGE} for create/edit/screens,
 * {@code FUNC_CAMPAIGN_PUBLISH} for publish/pause/archive.
 * <p>
 * Note (V1 gap): the BFF relays runtime routes without requiring a token, so a
 * signed-in user's AUTHENTICATED-audience campaigns only surface here if the
 * frontend sends its bearer token on {@code GET /api/campaigns/active} anyway
 * (the token-relay filter forwards it when present).
 */
@RestController
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    // ── runtime (pre-auth) ────────────────────────────────────────────────

    @GetMapping("/api/campaigns/active")
    public List<CampaignRuntimeResponse> getActiveCampaigns() {
        return campaignService.getActiveCampaigns();
    }

    @PostMapping("/api/campaigns/{campaignId}/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordCampaignEvent(@PathVariable UUID campaignId, @RequestBody CampaignEventRequest request) {
        campaignService.recordCampaignEvent(campaignId, request);
    }

    // ── admin: reads (FUNC_CAMPAIGN_READ) ─────────────────────────────────

    @GetMapping("/api/campaigns/admin")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_READ')")
    public List<CampaignResponse> listCampaigns() {
        return campaignService.listCampaigns();
    }

    @GetMapping("/api/campaigns/admin/{campaignId}")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_READ')")
    public CampaignResponse getCampaign(@PathVariable UUID campaignId) {
        return campaignService.getCampaign(campaignId);
    }

    // ── admin: create / edit (FUNC_CAMPAIGN_MANAGE) ───────────────────────

    @PostMapping("/api/campaigns/admin")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignResponse createCampaign(@RequestBody CampaignRequest request) {
        return campaignService.createCampaign(request);
    }

    @PutMapping("/api/campaigns/admin/{campaignId}")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_MANAGE')")
    public CampaignResponse updateCampaign(@PathVariable UUID campaignId, @RequestBody CampaignRequest request) {
        return campaignService.updateCampaign(campaignId, request);
    }

    @DeleteMapping("/api/campaigns/admin/{campaignId}")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCampaign(@PathVariable UUID campaignId) {
        campaignService.deleteCampaign(campaignId);
    }

    // ── admin: lifecycle (FUNC_CAMPAIGN_PUBLISH) ──────────────────────────

    @PostMapping("/api/campaigns/admin/{campaignId}/publish")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_PUBLISH')")
    public CampaignResponse publishCampaign(@PathVariable UUID campaignId) {
        return campaignService.publishCampaign(campaignId);
    }

    @PostMapping("/api/campaigns/admin/{campaignId}/pause")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_PUBLISH')")
    public CampaignResponse pauseCampaign(@PathVariable UUID campaignId) {
        return campaignService.pauseCampaign(campaignId);
    }

    @PostMapping("/api/campaigns/admin/{campaignId}/archive")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_PUBLISH')")
    public CampaignResponse archiveCampaign(@PathVariable UUID campaignId) {
        return campaignService.archiveCampaign(campaignId);
    }

    // ── admin: screens (FUNC_CAMPAIGN_MANAGE) ─────────────────────────────

    @PostMapping("/api/campaigns/admin/{campaignId}/screens")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignScreenResponse addCampaignScreen(@PathVariable UUID campaignId,
                                                    @RequestBody CampaignScreenRequest request) {
        return campaignService.addCampaignScreen(campaignId, request);
    }

    @PutMapping("/api/campaigns/admin/{campaignId}/screens/reorder")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_MANAGE')")
    public CampaignResponse reorderCampaignScreens(@PathVariable UUID campaignId,
                                                   @RequestBody ReorderCampaignScreensRequest request) {
        return campaignService.reorderCampaignScreens(campaignId, request);
    }

    @PutMapping("/api/campaigns/admin/{campaignId}/screens/{screenId}")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_MANAGE')")
    public CampaignScreenResponse updateCampaignScreen(@PathVariable UUID campaignId, @PathVariable UUID screenId,
                                                       @RequestBody CampaignScreenRequest request) {
        return campaignService.updateCampaignScreen(campaignId, screenId, request);
    }

    @DeleteMapping("/api/campaigns/admin/{campaignId}/screens/{screenId}")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCampaignScreen(@PathVariable UUID campaignId, @PathVariable UUID screenId) {
        campaignService.removeCampaignScreen(campaignId, screenId);
    }

    @PostMapping("/api/campaigns/admin/{campaignId}/screens/{screenId}/image")
    @PreAuthorize("hasAuthority('FUNC_CAMPAIGN_MANAGE')")
    public CampaignScreenResponse uploadCampaignScreenImage(@PathVariable UUID campaignId, @PathVariable UUID screenId,
                                                            @RequestParam("file") MultipartFile file,
                                                            @RequestParam(value = "focalPointX", required = false) Float focalPointX,
                                                            @RequestParam(value = "focalPointY", required = false) Float focalPointY) {
        return campaignService.uploadCampaignScreenImage(campaignId, screenId, file, focalPointX, focalPointY);
    }
}
