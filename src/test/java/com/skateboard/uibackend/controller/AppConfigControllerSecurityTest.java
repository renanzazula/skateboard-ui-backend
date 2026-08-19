package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.BrandingConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeFeaturedPlayerConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigMode;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicConfigResponse;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.AppConfigService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the BFF's own authentication/authorization gate for the branding
 * routes — not skateboard-app-config-be's, which is a separate service
 * exercised via AppConfigClient (mocked out here through AppConfigService).
 * Mirrors {@link PodcastControllerSecurityTest}, plus one case specific to
 * this controller: {@code GET /api/config} is intentionally public.
 */
@WebMvcTest(controllers = AppConfigController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class AppConfigControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppConfigService appConfigService;

    @Test
    void allowsAnonymousAccessToThePublicConfigEndpoint() throws Exception {
        given(appConfigService.getPublicConfig()).willReturn(new PublicConfigResponse().loginBackgroundVersion(0));

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginBackgroundVersion").value(0));
    }

    @Test
    void rejectsManagementRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/config/branding"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsATokenMissingTheRequiredAuthority() throws Exception {
        mockMvc.perform(get("/api/config/branding").with(jwt().authorities(() -> "FUNC_SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsATokenWithTheRequiredAuthority() throws Exception {
        given(appConfigService.getBrandingConfig()).willReturn(new BrandingConfigResponse().loginBackgroundVersion(0));

        mockMvc.perform(get("/api/config/branding").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS_BRANDING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginBackgroundVersion").value(0));
    }

    // ── Home category config admin endpoints (FUNC_HOME_CATEGORY_CONFIG) ────

    @Test
    void rejectsHomeCategoryConfigWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/config/home/video-categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsHomeCategoryConfigWithoutTheRequiredAuthority() throws Exception {
        mockMvc.perform(get("/api/config/home/video-categories").with(jwt().authorities(() -> "FUNC_TAB_HOME")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsReadingHomeCategoryConfigWithTheRequiredAuthority() throws Exception {
        given(appConfigService.getHomeVideoCategoryConfig())
                .willReturn(new HomeVideoCategoryConfigResponse().mode(HomeVideoCategoryConfigMode.ALL));

        mockMvc.perform(get("/api/config/home/video-categories").with(jwt().authorities(() -> "FUNC_HOME_CATEGORY_CONFIG")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("ALL"));
    }

    @Test
    void rejectsUpdatingHomeCategoryConfigWithoutTheRequiredAuthority() throws Exception {
        mockMvc.perform(put("/api/config/home/video-categories")
                        .with(jwt().authorities(() -> "FUNC_TAB_HOME"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"ALL\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsUpdatingHomeCategoryConfigWithTheRequiredAuthority() throws Exception {
        given(appConfigService.updateHomeVideoCategoryConfig(any()))
                .willReturn(new HomeVideoCategoryConfigResponse().mode(HomeVideoCategoryConfigMode.ALL));

        mockMvc.perform(put("/api/config/home/video-categories")
                        .with(jwt().authorities(() -> "FUNC_HOME_CATEGORY_CONFIG"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"ALL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("ALL"));
    }

    // ── Home Featured Player admin endpoints (FUNC_HOME_FEATURED_PLAYER_CONFIG) ──

    @Test
    void rejectsHomeFeaturedPlayerConfigWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/config/home/featured-player"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsHomeFeaturedPlayerConfigWithoutTheRequiredAuthority() throws Exception {
        mockMvc.perform(get("/api/config/home/featured-player").with(jwt().authorities(() -> "FUNC_TAB_HOME")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsReadingHomeFeaturedPlayerConfigWithTheRequiredAuthority() throws Exception {
        given(appConfigService.getHomeFeaturedPlayerConfig())
                .willReturn(new HomeFeaturedPlayerConfigResponse().enabled(false));

        mockMvc.perform(get("/api/config/home/featured-player").with(jwt().authorities(() -> "FUNC_HOME_FEATURED_PLAYER_CONFIG")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void rejectsUpdatingHomeFeaturedPlayerConfigWithoutTheRequiredAuthority() throws Exception {
        mockMvc.perform(put("/api/config/home/featured-player")
                        .with(jwt().authorities(() -> "FUNC_TAB_HOME"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"playerType\":\"MINI\",\"position\":\"BOTTOM\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsUpdatingHomeFeaturedPlayerConfigWithTheRequiredAuthority() throws Exception {
        given(appConfigService.updateHomeFeaturedPlayerConfig(any()))
                .willReturn(new HomeFeaturedPlayerConfigResponse().enabled(false));

        mockMvc.perform(put("/api/config/home/featured-player")
                        .with(jwt().authorities(() -> "FUNC_HOME_FEATURED_PLAYER_CONFIG"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"playerType\":\"MINI\",\"position\":\"BOTTOM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}
