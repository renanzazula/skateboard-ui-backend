package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.CampaignClient;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignEventRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRuntimeResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.ReorderCampaignScreensRequest;
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
 * {@link CampaignService} is a thin passthrough to {@link CampaignClient} —
 * mirrors {@link AppConfigServiceTest}'s shape.
 */
class CampaignServiceTest {

    @Mock
    private CampaignClient campaignClient;

    private CampaignService service;

    private final UUID id = UUID.randomUUID();
    private final UUID screenId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CampaignService(campaignClient);
    }

    @Test
    void getActiveCampaignsDelegates() {
        List<CampaignRuntimeResponse> response = List.of(new CampaignRuntimeResponse().id(id));
        when(campaignClient.getActiveCampaigns()).thenReturn(response);

        assertThat(service.getActiveCampaigns()).isSameAs(response);
    }

    @Test
    void recordCampaignEventDelegates() {
        CampaignEventRequest request = new CampaignEventRequest();

        service.recordCampaignEvent(id, request);

        verify(campaignClient).recordCampaignEvent(id, request);
    }

    @Test
    void listCampaignsDelegates() {
        List<CampaignResponse> response = List.of(new CampaignResponse().id(id));
        when(campaignClient.listCampaigns()).thenReturn(response);

        assertThat(service.listCampaigns()).isSameAs(response);
    }

    @Test
    void getCampaignDelegates() {
        CampaignResponse response = new CampaignResponse().id(id);
        when(campaignClient.getCampaign(id)).thenReturn(response);

        assertThat(service.getCampaign(id)).isSameAs(response);
    }

    @Test
    void createCampaignDelegates() {
        CampaignRequest request = new CampaignRequest();
        CampaignResponse response = new CampaignResponse().id(id);
        when(campaignClient.createCampaign(request)).thenReturn(response);

        assertThat(service.createCampaign(request)).isSameAs(response);
    }

    @Test
    void updateCampaignDelegates() {
        CampaignRequest request = new CampaignRequest();
        CampaignResponse response = new CampaignResponse().id(id);
        when(campaignClient.updateCampaign(id, request)).thenReturn(response);

        assertThat(service.updateCampaign(id, request)).isSameAs(response);
    }

    @Test
    void deleteCampaignDelegates() {
        service.deleteCampaign(id);

        verify(campaignClient).deleteCampaign(id);
    }

    @Test
    void publishCampaignDelegates() {
        CampaignResponse response = new CampaignResponse().id(id);
        when(campaignClient.publishCampaign(id)).thenReturn(response);

        assertThat(service.publishCampaign(id)).isSameAs(response);
    }

    @Test
    void pauseCampaignDelegates() {
        CampaignResponse response = new CampaignResponse().id(id);
        when(campaignClient.pauseCampaign(id)).thenReturn(response);

        assertThat(service.pauseCampaign(id)).isSameAs(response);
    }

    @Test
    void archiveCampaignDelegates() {
        CampaignResponse response = new CampaignResponse().id(id);
        when(campaignClient.archiveCampaign(id)).thenReturn(response);

        assertThat(service.archiveCampaign(id)).isSameAs(response);
    }

    @Test
    void addCampaignScreenDelegates() {
        CampaignScreenRequest request = new CampaignScreenRequest();
        CampaignScreenResponse response = new CampaignScreenResponse();
        when(campaignClient.addCampaignScreen(id, request)).thenReturn(response);

        assertThat(service.addCampaignScreen(id, request)).isSameAs(response);
    }

    @Test
    void reorderCampaignScreensDelegates() {
        ReorderCampaignScreensRequest request = new ReorderCampaignScreensRequest();
        CampaignResponse response = new CampaignResponse().id(id);
        when(campaignClient.reorderCampaignScreens(id, request)).thenReturn(response);

        assertThat(service.reorderCampaignScreens(id, request)).isSameAs(response);
    }

    @Test
    void updateCampaignScreenDelegates() {
        CampaignScreenRequest request = new CampaignScreenRequest();
        CampaignScreenResponse response = new CampaignScreenResponse();
        when(campaignClient.updateCampaignScreen(id, screenId, request)).thenReturn(response);

        assertThat(service.updateCampaignScreen(id, screenId, request)).isSameAs(response);
    }

    @Test
    void removeCampaignScreenDelegates() {
        service.removeCampaignScreen(id, screenId);

        verify(campaignClient).removeCampaignScreen(id, screenId);
    }

    @Test
    void uploadCampaignScreenImageDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        CampaignScreenResponse response = new CampaignScreenResponse();
        when(campaignClient.uploadCampaignScreenImage(id, screenId, file, 0.5f, 0.25f)).thenReturn(response);

        assertThat(service.uploadCampaignScreenImage(id, screenId, file, 0.5f, 0.25f)).isSameAs(response);
    }
}
