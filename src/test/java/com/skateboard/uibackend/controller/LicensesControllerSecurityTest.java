package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.LicensesResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.LicensesStatus;
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
 * The BFF's own auth gate for the Open-source Licenses routes — not
 * skateboard-app-config-be's. Mirrors {@link AboutUsControllerSecurityTest}:
 * the viewer GET is open to any user with FUNC_TAB_SETTINGS; the draft read
 * and save require FUNC_LICENSES_MANAGE.
 */
@WebMvcTest(controllers = LicensesController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class LicensesControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppConfigService appConfigService;

    private static LicensesResponse publishedPage() {
        return new LicensesResponse().title("Open-source Licenses").body("This app uses...").status(LicensesStatus.PUBLISHED);
    }

    // ── GET /api/licenses (FUNC_TAB_SETTINGS) ───────────────────────────

    @Test
    void rejectsTheViewerWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/licenses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsTheViewerWithoutFuncTabSettings() throws Exception {
        mockMvc.perform(get("/api/licenses").with(jwt().authorities(() -> "FUNC_SOMETHING_ELSE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void servesThePublishedPageToAnyUserWithFuncTabSettings() throws Exception {
        given(appConfigService.getLicenses()).willReturn(publishedPage());

        mockMvc.perform(get("/api/licenses").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Open-source Licenses"))
                .andExpect(jsonPath("$.status").value("published"));
    }

    @Test
    void returns204WhenNothingIsPublished() throws Exception {
        given(appConfigService.getLicenses()).willReturn(null);

        mockMvc.perform(get("/api/licenses").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS")))
                .andExpect(status().isNoContent());
    }

    // ── admin routes (FUNC_LICENSES_MANAGE) ─────────────────────────────

    @Test
    void rejectsTheDraftReadWithOnlyFuncTabSettings() throws Exception {
        mockMvc.perform(get("/api/licenses/admin").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsTheDraftReadWithFuncLicensesManage() throws Exception {
        given(appConfigService.getLicensesAdmin()).willReturn(publishedPage());

        mockMvc.perform(get("/api/licenses/admin").with(jwt().authorities(() -> "FUNC_LICENSES_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Open-source Licenses"));
    }

    @Test
    void rejectsSaveWithoutFuncLicensesManage() throws Exception {
        mockMvc.perform(put("/api/licenses")
                        .with(jwt().authorities(() -> "FUNC_TAB_SETTINGS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Open-source Licenses\",\"body\":\"This app uses...\",\"status\":\"draft\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsSaveWithFuncLicensesManage() throws Exception {
        given(appConfigService.updateLicenses(any())).willReturn(publishedPage());

        mockMvc.perform(put("/api/licenses")
                        .with(jwt().authorities(() -> "FUNC_LICENSES_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Open-source Licenses\",\"body\":\"This app uses...\",\"status\":\"published\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"));
    }
}
