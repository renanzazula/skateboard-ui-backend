package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.TermsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.TermsStatus;
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
 * The BFF's own auth gate for the Terms &amp; Conditions routes — not
 * skateboard-app-config-be's. Mirrors {@link AboutUsControllerSecurityTest}:
 * the viewer GET is open to any user with FUNC_TAB_SETTINGS; the draft read
 * and save require FUNC_TERMS_MANAGE.
 */
@WebMvcTest(controllers = TermsController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class TermsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppConfigService appConfigService;

    private static TermsResponse publishedPage() {
        return new TermsResponse().title("Terms & Conditions").body("By using this app...").status(TermsStatus.PUBLISHED);
    }

    // ── GET /api/terms (FUNC_TAB_SETTINGS) ──────────────────────────────

    @Test
    void rejectsTheViewerWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/terms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsTheViewerWithoutFuncTabSettings() throws Exception {
        mockMvc.perform(get("/api/terms").with(jwt().authorities(() -> "FUNC_SOMETHING_ELSE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void servesThePublishedPageToAnyUserWithFuncTabSettings() throws Exception {
        given(appConfigService.getTerms()).willReturn(publishedPage());

        mockMvc.perform(get("/api/terms").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Terms & Conditions"))
                .andExpect(jsonPath("$.status").value("published"));
    }

    @Test
    void returns204WhenNothingIsPublished() throws Exception {
        given(appConfigService.getTerms()).willReturn(null);

        mockMvc.perform(get("/api/terms").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS")))
                .andExpect(status().isNoContent());
    }

    // ── admin routes (FUNC_TERMS_MANAGE) ────────────────────────────────

    @Test
    void rejectsTheDraftReadWithOnlyFuncTabSettings() throws Exception {
        mockMvc.perform(get("/api/terms/admin").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsTheDraftReadWithFuncTermsManage() throws Exception {
        given(appConfigService.getTermsAdmin()).willReturn(publishedPage());

        mockMvc.perform(get("/api/terms/admin").with(jwt().authorities(() -> "FUNC_TERMS_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Terms & Conditions"));
    }

    @Test
    void rejectsSaveWithoutFuncTermsManage() throws Exception {
        mockMvc.perform(put("/api/terms")
                        .with(jwt().authorities(() -> "FUNC_TAB_SETTINGS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Terms & Conditions\",\"body\":\"By using this app...\",\"status\":\"draft\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsSaveWithFuncTermsManage() throws Exception {
        given(appConfigService.updateTerms(any())).willReturn(publishedPage());

        mockMvc.perform(put("/api/terms")
                        .with(jwt().authorities(() -> "FUNC_TERMS_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Terms & Conditions\",\"body\":\"By using this app...\",\"status\":\"published\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"));
    }
}
