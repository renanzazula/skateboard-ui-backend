package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.PrivacyPolicyResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PrivacyPolicyStatus;
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
 * The BFF's own auth gate for the Privacy Policy routes — not
 * skateboard-app-config-be's. Unlike {@link AboutUsControllerSecurityTest},
 * the viewer GET requires no token/authority at all here — it must be
 * reachable by app reviewers and signed-out users. The draft read and save
 * require FUNC_PRIVACY_POLICY_MANAGE.
 */
@WebMvcTest(controllers = PrivacyPolicyController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class PrivacyPolicyControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppConfigService appConfigService;

    private static PrivacyPolicyResponse publishedPage() {
        return new PrivacyPolicyResponse().title("Privacy Policy").body("We collect...").status(PrivacyPolicyStatus.PUBLISHED);
    }

    // ── GET /api/privacy-policy (fully anonymous) ──────────────────────────

    @Test
    void servesThePublishedPageWithNoTokenAtAll() throws Exception {
        given(appConfigService.getPrivacyPolicy()).willReturn(publishedPage());

        mockMvc.perform(get("/api/privacy-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Privacy Policy"))
                .andExpect(jsonPath("$.status").value("published"));
    }

    @Test
    void returns204WhenNothingIsPublishedWithNoTokenAtAll() throws Exception {
        given(appConfigService.getPrivacyPolicy()).willReturn(null);

        mockMvc.perform(get("/api/privacy-policy"))
                .andExpect(status().isNoContent());
    }

    // ── admin routes (FUNC_PRIVACY_POLICY_MANAGE) ──────────────────────────

    @Test
    void rejectsTheDraftReadWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/privacy-policy/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsTheDraftReadWithoutFuncPrivacyPolicyManage() throws Exception {
        mockMvc.perform(get("/api/privacy-policy/admin").with(jwt().authorities(() -> "FUNC_TAB_SETTINGS")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsTheDraftReadWithFuncPrivacyPolicyManage() throws Exception {
        given(appConfigService.getPrivacyPolicyAdmin()).willReturn(publishedPage());

        mockMvc.perform(get("/api/privacy-policy/admin").with(jwt().authorities(() -> "FUNC_PRIVACY_POLICY_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Privacy Policy"));
    }

    @Test
    void rejectsSaveWithoutFuncPrivacyPolicyManage() throws Exception {
        mockMvc.perform(put("/api/privacy-policy")
                        .with(jwt().authorities(() -> "FUNC_TAB_SETTINGS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Privacy Policy\",\"body\":\"We collect...\",\"status\":\"draft\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsSaveWithFuncPrivacyPolicyManage() throws Exception {
        given(appConfigService.updatePrivacyPolicy(any())).willReturn(publishedPage());

        mockMvc.perform(put("/api/privacy-policy")
                        .with(jwt().authorities(() -> "FUNC_PRIVACY_POLICY_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Privacy Policy\",\"body\":\"We collect...\",\"status\":\"published\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"));
    }
}
