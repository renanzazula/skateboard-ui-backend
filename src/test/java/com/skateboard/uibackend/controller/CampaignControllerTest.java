package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.CampaignEventRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRuntimeResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.ReorderCampaignScreensRequest;
import com.skateboard.uibackend.service.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CampaignController} just delegates to {@link CampaignService} — the
 * {@code @PreAuthorize} enforcement itself is covered by {@link
 * CampaignControllerSecurityTest}'s {@code WebMvcTest} suite; these plain
 * unit tests exist to exercise every route's delegation without paying for a
 * Spring context per test.
 */
class CampaignControllerTest {

    @Mock
    private CampaignService campaignService;

    private CampaignController controller;

    private final UUID campaignId = UUID.randomUUID();
    private final UUID screenId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new CampaignController(campaignService);
    }

    @Test
    void getActiveCampaignsDelegates() {
        List<CampaignRuntimeResponse> response = List.of(new CampaignRuntimeResponse().id(campaignId));
        when(campaignService.getActiveCampaigns()).thenReturn(response);

        assertThat(controller.getActiveCampaigns()).isSameAs(response);
    }

    @Test
    void recordCampaignEventDelegates() {
        CampaignEventRequest request = new CampaignEventRequest();

        controller.recordCampaignEvent(campaignId, request);

        verify(campaignService).recordCampaignEvent(campaignId, request);
    }

    @Test
    void listCampaignsDelegates() {
        List<CampaignResponse> response = List.of(new CampaignResponse().id(campaignId));
        when(campaignService.listCampaigns()).thenReturn(response);

        assertThat(controller.listCampaigns()).isSameAs(response);
    }

    @Test
    void getCampaignDelegates() {
        CampaignResponse response = new CampaignResponse().id(campaignId);
        when(campaignService.getCampaign(campaignId)).thenReturn(response);

        assertThat(controller.getCampaign(campaignId)).isSameAs(response);
    }

    @Test
    void createCampaignDelegates() {
        CampaignRequest request = new CampaignRequest();
        CampaignResponse response = new CampaignResponse().id(campaignId);
        when(campaignService.createCampaign(request)).thenReturn(response);

        assertThat(controller.createCampaign(request)).isSameAs(response);
    }

    @Test
    void updateCampaignDelegates() {
        CampaignRequest request = new CampaignRequest();
        CampaignResponse response = new CampaignResponse().id(campaignId);
        when(campaignService.updateCampaign(campaignId, request)).thenReturn(response);

        assertThat(controller.updateCampaign(campaignId, request)).isSameAs(response);
    }

    @Test
    void deleteCampaignDelegates() {
        controller.deleteCampaign(campaignId);

        verify(campaignService).deleteCampaign(campaignId);
    }

    @Test
    void publishCampaignDelegates() {
        CampaignResponse response = new CampaignResponse().id(campaignId);
        when(campaignService.publishCampaign(campaignId)).thenReturn(response);

        assertThat(controller.publishCampaign(campaignId)).isSameAs(response);
    }

    @Test
    void pauseCampaignDelegates() {
        CampaignResponse response = new CampaignResponse().id(campaignId);
        when(campaignService.pauseCampaign(campaignId)).thenReturn(response);

        assertThat(controller.pauseCampaign(campaignId)).isSameAs(response);
    }

    @Test
    void archiveCampaignDelegates() {
        CampaignResponse response = new CampaignResponse().id(campaignId);
        when(campaignService.archiveCampaign(campaignId)).thenReturn(response);

        assertThat(controller.archiveCampaign(campaignId)).isSameAs(response);
    }

    @Test
    void addCampaignScreenDelegates() {
        CampaignScreenRequest request = new CampaignScreenRequest();
        CampaignScreenResponse response = new CampaignScreenResponse();
        when(campaignService.addCampaignScreen(campaignId, request)).thenReturn(response);

        assertThat(controller.addCampaignScreen(campaignId, request)).isSameAs(response);
    }

    @Test
    void reorderCampaignScreensDelegates() {
        ReorderCampaignScreensRequest request = new ReorderCampaignScreensRequest();
        CampaignResponse response = new CampaignResponse().id(campaignId);
        when(campaignService.reorderCampaignScreens(campaignId, request)).thenReturn(response);

        assertThat(controller.reorderCampaignScreens(campaignId, request)).isSameAs(response);
    }

    @Test
    void updateCampaignScreenDelegates() {
        CampaignScreenRequest request = new CampaignScreenRequest();
        CampaignScreenResponse response = new CampaignScreenResponse();
        when(campaignService.updateCampaignScreen(campaignId, screenId, request)).thenReturn(response);

        assertThat(controller.updateCampaignScreen(campaignId, screenId, request)).isSameAs(response);
    }

    @Test
    void removeCampaignScreenDelegates() {
        controller.removeCampaignScreen(campaignId, screenId);

        verify(campaignService).removeCampaignScreen(campaignId, screenId);
    }

    @Test
    void uploadCampaignScreenImageDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        CampaignScreenResponse response = new CampaignScreenResponse();
        when(campaignService.uploadCampaignScreenImage(campaignId, screenId, file, 0.5f, 0.25f)).thenReturn(response);

        assertThat(controller.uploadCampaignScreenImage(campaignId, screenId, file, 0.5f, 0.25f)).isSameAs(response);
    }
}
