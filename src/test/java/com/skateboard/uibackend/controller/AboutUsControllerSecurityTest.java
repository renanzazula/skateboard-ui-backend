package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.AboutImageResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.AboutPageResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.AboutPageStatus;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.AppConfigService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The BFF's own auth gate for the About Us routes — not skateboard-app-config-be's.
 * Mirrors {@link AppConfigControllerSecurityTest}: the viewer GET is open to any
 * user with FUNC_TAB_SETTINGS; the draft read, save and image upload require
 * FUNC_ABOUT_US_MANAGE.
 */
@WebMvcTest(controllers = AboutUsController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class AboutUsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppConfigService appConfigService;

    private static AboutPageResponse publishedPage() {
        return new AboutPageResponse().title("About Us").status(AboutPageStatus.PUBLISHED);
    }

    // ── GET /api/about-us (FUNC_TAB_SETTINGS) ──────────────────────────────

    @Test
    void rejectsTheViewerWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/about-us"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsTheViewerWithoutFuncTabSettings() throws Exception {
        mockMvc.perform(get("/api/about-us").with(jwt().authorities(() -> "FUNC_SOMETHING_ELSE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void servesThePublishedPageToAnyUserWithFuncTabSettings() throws Exception {
        given(appConfigService.getAboutUs()).willReturn(publishedPage());

        mockMvc.perform(get("/api/about-us").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("About Us"))
                .andExpect(jsonPath("$.status").value("published"));
    }

    @Test
    void returns204WhenNothingIsPublished() throws Exception {
        given(appConfigService.getAboutUs()).willReturn(null);

        mockMvc.perform(get("/api/about-us").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS")))
                .andExpect(status().isNoContent());
    }

    // ── admin routes (FUNC_ABOUT_US_MANAGE) ────────────────────────────────

    @Test
    void rejectsTheDraftReadWithOnlyFuncTabSettings() throws Exception {
        mockMvc.perform(get("/api/about-us/admin").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsTheDraftReadWithFuncAboutUsManage() throws Exception {
        given(appConfigService.getAboutUsAdmin()).willReturn(publishedPage());

        mockMvc.perform(get("/api/about-us/admin").with(jwt().authorities(() -> "FUNC_ABOUT_US_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("About Us"));
    }

    @Test
    void rejectsSaveWithoutFuncAboutUsManage() throws Exception {
        mockMvc.perform(put("/api/about-us")
                        .with(jwt().authorities(() -> "FUNC_TAB_SETTINGS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"About Us\",\"status\":\"draft\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsSaveWithFuncAboutUsManage() throws Exception {
        given(appConfigService.updateAboutUs(any())).willReturn(publishedPage());

        mockMvc.perform(put("/api/about-us")
                        .with(jwt().authorities(() -> "FUNC_ABOUT_US_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"About Us\",\"status\":\"published\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"));
    }

    @Test
    void allowsImageUploadWithFuncAboutUsManage() throws Exception {
        given(appConfigService.uploadAboutUsImage(any()))
                .willReturn(new AboutImageResponse().url("https://cdn.example/about-us/x.jpg"));

        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/about-us/images").file(file)
                        .with(jwt().authorities(() -> "FUNC_ABOUT_US_MANAGE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://cdn.example/about-us/x.jpg"));
    }
}
