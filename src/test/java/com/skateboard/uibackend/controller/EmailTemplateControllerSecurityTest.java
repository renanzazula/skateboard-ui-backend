package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.EmailTemplateResponse;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.EmailTemplateService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The BFF's own auth gate for the email-templates routes — not
 * skateboard-app-config-be's. Every route here requires
 * FUNC_EMAIL_TEMPLATE_MANAGE; there is no public/viewer route, unlike
 * {@link AboutUsControllerSecurityTest}.
 */
@WebMvcTest(controllers = EmailTemplateController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class EmailTemplateControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailTemplateService emailTemplateService;

    // ── GET /api/email-templates ────────────────────────────────────────────

    @Test
    void rejectsListWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/email-templates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsListWithoutFuncEmailTemplateManage() throws Exception {
        mockMvc.perform(get("/api/email-templates").with(jwt().authorities(() -> "FUNC_SOMETHING_ELSE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsListWithFuncEmailTemplateManage() throws Exception {
        given(emailTemplateService.list()).willReturn(List.of(new EmailTemplateResponse().subject("Hi")));

        mockMvc.perform(get("/api/email-templates").with(jwt().authorities(() -> "FUNC_EMAIL_TEMPLATE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Hi"));
    }

    // ── GET /api/email-templates/{type}/{language} ──────────────────────────

    @Test
    void rejectsGetWithoutFuncEmailTemplateManage() throws Exception {
        mockMvc.perform(get("/api/email-templates/GUEST_APPLICATION_RECEIVED/en")
                        .with(jwt().authorities(() -> "FUNC_SOMETHING_ELSE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsGetWithFuncEmailTemplateManage() throws Exception {
        given(emailTemplateService.get(eq("GUEST_APPLICATION_RECEIVED"), eq("en")))
                .willReturn(new EmailTemplateResponse().subject("Hi {{name}}"));

        mockMvc.perform(get("/api/email-templates/GUEST_APPLICATION_RECEIVED/en")
                        .with(jwt().authorities(() -> "FUNC_EMAIL_TEMPLATE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Hi {{name}}"));
    }

    // ── PUT /api/email-templates/{id} ───────────────────────────────────────

    @Test
    void rejectsUpdateWithoutFuncEmailTemplateManage() throws Exception {
        mockMvc.perform(put("/api/email-templates/" + UUID.randomUUID())
                        .with(jwt().authorities(() -> "FUNC_SOMETHING_ELSE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Hi\",\"body\":\"Body\",\"enabled\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsUpdateWithFuncEmailTemplateManage() throws Exception {
        given(emailTemplateService.update(any(), any())).willReturn(new EmailTemplateResponse().subject("New subject"));

        mockMvc.perform(put("/api/email-templates/" + UUID.randomUUID())
                        .with(jwt().authorities(() -> "FUNC_EMAIL_TEMPLATE_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"New subject\",\"body\":\"Body\",\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("New subject"));
    }
}
