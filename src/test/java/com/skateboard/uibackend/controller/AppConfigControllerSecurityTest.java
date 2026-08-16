package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.BrandingConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicConfigResponse;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.AppConfigService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
