package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.CampaignResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRuntimeResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignStatus;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.CampaignService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The BFF's own auth gate for the campaign routes — not skateboard-app-config-be's.
 * {@code GET /api/campaigns/active} and {@code POST /api/campaigns/{id}/events}
 * are open (pre-auth); the admin reads need {@code FUNC_CAMPAIGN_READ}, the
 * create/edit/screen routes {@code FUNC_CAMPAIGN_MANAGE}, and publish/pause/
 * archive {@code FUNC_CAMPAIGN_PUBLISH}. Mirrors {@link AboutUsControllerSecurityTest}.
 */
@WebMvcTest(controllers = CampaignController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class CampaignControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CampaignService campaignService;

    private static final UUID CAMPAIGN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SCREEN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static CampaignResponse draftCampaign() {
        return new CampaignResponse().id(CAMPAIGN_ID).name("Launch week").status(CampaignStatus.DRAFT);
    }

    // ── runtime routes are pre-auth ───────────────────────────────────────

    @Test
    void servesActiveCampaignsWithoutAToken() throws Exception {
        given(campaignService.getActiveCampaigns())
                .willReturn(List.of(new CampaignRuntimeResponse().id(CAMPAIGN_ID).priority(10)));

        mockMvc.perform(get("/api/campaigns/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].priority").value(10));
    }

    @Test
    void acceptsAnEventWithoutAToken() throws Exception {
        mockMvc.perform(post("/api/campaigns/{id}/events", CAMPAIGN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"CAMPAIGN_CLOSED\"}"))
                .andExpect(status().isNoContent());

        verify(campaignService).recordCampaignEvent(any(), any());
    }

    // ── admin reads: FUNC_CAMPAIGN_READ ───────────────────────────────────

    @Test
    void rejectsTheAdminListWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/campaigns/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsTheAdminListWithTheWrongAuthority() throws Exception {
        mockMvc.perform(get("/api/campaigns/admin").with(jwt().authorities(() -> "FUNC_SOMETHING_ELSE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsTheAdminListWithFuncCampaignRead() throws Exception {
        given(campaignService.listCampaigns()).willReturn(List.of(draftCampaign()));

        mockMvc.perform(get("/api/campaigns/admin").with(jwt().authorities(() -> "FUNC_CAMPAIGN_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Launch week"));
    }

    // ── create/edit/screens: FUNC_CAMPAIGN_MANAGE ─────────────────────────

    @Test
    void rejectsCreateWithOnlyFuncCampaignRead() throws Exception {
        mockMvc.perform(post("/api/campaigns/admin")
                        .with(jwt().authorities(() -> "FUNC_CAMPAIGN_READ"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"startAt\":\"2026-01-01T00:00:00Z\",\"endAt\":\"2026-02-01T00:00:00Z\",\"priority\":1,\"audience\":\"ALL\",\"frequencyType\":\"ALWAYS\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsCreateWithFuncCampaignManage() throws Exception {
        given(campaignService.createCampaign(any())).willReturn(draftCampaign());

        mockMvc.perform(post("/api/campaigns/admin")
                        .with(jwt().authorities(() -> "FUNC_CAMPAIGN_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Launch week\",\"startAt\":\"2026-01-01T00:00:00Z\",\"endAt\":\"2026-02-01T00:00:00Z\",\"priority\":1,\"audience\":\"ALL\",\"frequencyType\":\"ALWAYS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void rejectsScreenRemovalWithoutFuncCampaignManage() throws Exception {
        mockMvc.perform(delete("/api/campaigns/admin/{id}/screens/{screenId}", CAMPAIGN_ID, SCREEN_ID)
                        .with(jwt().authorities(() -> "FUNC_CAMPAIGN_READ")))
                .andExpect(status().isForbidden());
    }

    // ── publish/pause/archive: FUNC_CAMPAIGN_PUBLISH ──────────────────────

    @Test
    void rejectsPublishWithOnlyFuncCampaignManage() throws Exception {
        mockMvc.perform(post("/api/campaigns/admin/{id}/publish", CAMPAIGN_ID)
                        .with(jwt().authorities(() -> "FUNC_CAMPAIGN_MANAGE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsPublishWithFuncCampaignPublish() throws Exception {
        given(campaignService.publishCampaign(CAMPAIGN_ID))
                .willReturn(new CampaignResponse().id(CAMPAIGN_ID).name("Launch week").status(CampaignStatus.ACTIVE));

        mockMvc.perform(post("/api/campaigns/admin/{id}/publish", CAMPAIGN_ID)
                        .with(jwt().authorities(() -> "FUNC_CAMPAIGN_PUBLISH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
