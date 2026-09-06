package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.CampaignClient;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignEventRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRuntimeResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.ReorderCampaignScreensRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Thin pass-through today — same seam as {@link AppConfigService} for future
 * orchestration. Campaign business rules stay in skateboard-app-config-be
 * (spec §16); this BFF only relays the frontend's calls and its token.
 */
@Service
public class CampaignService {

    private final CampaignClient campaignClient;

    public CampaignService(CampaignClient campaignClient) {
        this.campaignClient = campaignClient;
    }

    public List<CampaignRuntimeResponse> getActiveCampaigns() {
        return campaignClient.getActiveCampaigns();
    }

    public void recordCampaignEvent(UUID campaignId, CampaignEventRequest request) {
        campaignClient.recordCampaignEvent(campaignId, request);
    }

    public List<CampaignResponse> listCampaigns() {
        return campaignClient.listCampaigns();
    }

    public CampaignResponse getCampaign(UUID campaignId) {
        return campaignClient.getCampaign(campaignId);
    }

    public CampaignResponse createCampaign(CampaignRequest request) {
        return campaignClient.createCampaign(request);
    }

    public CampaignResponse updateCampaign(UUID campaignId, CampaignRequest request) {
        return campaignClient.updateCampaign(campaignId, request);
    }

    public void deleteCampaign(UUID campaignId) {
        campaignClient.deleteCampaign(campaignId);
    }

    public CampaignResponse publishCampaign(UUID campaignId) {
        return campaignClient.publishCampaign(campaignId);
    }

    public CampaignResponse pauseCampaign(UUID campaignId) {
        return campaignClient.pauseCampaign(campaignId);
    }

    public CampaignResponse archiveCampaign(UUID campaignId) {
        return campaignClient.archiveCampaign(campaignId);
    }

    public CampaignScreenResponse addCampaignScreen(UUID campaignId, CampaignScreenRequest request) {
        return campaignClient.addCampaignScreen(campaignId, request);
    }

    public CampaignResponse reorderCampaignScreens(UUID campaignId, ReorderCampaignScreensRequest request) {
        return campaignClient.reorderCampaignScreens(campaignId, request);
    }

    public CampaignScreenResponse updateCampaignScreen(UUID campaignId, UUID screenId, CampaignScreenRequest request) {
        return campaignClient.updateCampaignScreen(campaignId, screenId, request);
    }

    public void removeCampaignScreen(UUID campaignId, UUID screenId) {
        campaignClient.removeCampaignScreen(campaignId, screenId);
    }

    public CampaignScreenResponse uploadCampaignScreenImage(UUID campaignId, UUID screenId, MultipartFile file,
                                                            Float focalPointX, Float focalPointY) {
        return campaignClient.uploadCampaignScreenImage(campaignId, screenId, file, focalPointX, focalPointY);
    }
}
