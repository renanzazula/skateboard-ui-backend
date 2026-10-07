package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.GuestApplicationSettingsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicGuestApplicationSettingsResponse;
import com.skateboard.uibackend.client.podcast.generated.model.GuestApplicationPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.GuestApplicationResponse;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.GuestApplicationService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the BFF's own authentication/authorization gate, mirroring
 * {@link PodcastControllerSecurityTest}'s shape. Every route except {@code
 * GET /api/guest-application-settings} requires its own FUNC_* authority;
 * that one route requires only a valid token (no x-required-permissions on
 * app-config-be's "public" settings endpoint).
 */
@WebMvcTest(controllers = GuestApplicationController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class GuestApplicationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GuestApplicationService guestApplicationService;

    // ── submit (FUNC_GUEST_APPLICATION_CREATE) ──────────────────────────────

    @Test
    void rejectsSubmitWithoutAToken() throws Exception {
        mockMvc.perform(post("/api/guest-applications").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsSubmitWithoutTheCreateAuthority() throws Exception {
        mockMvc.perform(post("/api/guest-applications").contentType("application/json").content("{}")
                        .with(jwt().authorities(() -> "FUNC_SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsSubmitWithTheCreateAuthority() throws Exception {
        UUID id = UUID.randomUUID();
        given(guestApplicationService.submit(any())).willReturn(new GuestApplicationResponse().id(id));

        mockMvc.perform(post("/api/guest-applications").contentType("application/json").content("{}")
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_CREATE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    // ── own application (FUNC_GUEST_APPLICATION_READ_OWN) ──────────────────

    @Test
    void rejectsGetMineWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/guest-applications/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsGetMineWithoutTheReadOwnAuthority() throws Exception {
        mockMvc.perform(get("/api/guest-applications/me").with(jwt().authorities(() -> "FUNC_SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsGetMineWithTheReadOwnAuthority() throws Exception {
        given(guestApplicationService.getMine()).willReturn(new GuestApplicationResponse().name("Jane Doe"));

        mockMvc.perform(get("/api/guest-applications/me").with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_READ_OWN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    // ── admin list/detail/status (FUNC_GUEST_APPLICATION_MANAGE) ────────────

    @Test
    void rejectsAdminListWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/admin/guest-applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsAdminListWithoutTheManageAuthority() throws Exception {
        mockMvc.perform(get("/api/admin/guest-applications")
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_READ_OWN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsAdminListWithTheManageAuthority() throws Exception {
        given(guestApplicationService.getAll(any(), any(), any()))
                .willReturn(new GuestApplicationPageResponse().page(0).size(10).total(0L));

        mockMvc.perform(get("/api/admin/guest-applications")
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void rejectsAdminDetailWithoutTheManageAuthority() throws Exception {
        mockMvc.perform(get("/api/admin/guest-applications/{id}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_READ_OWN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsAdminDetailWithTheManageAuthority() throws Exception {
        UUID id = UUID.randomUUID();
        given(guestApplicationService.getById(id)).willReturn(new GuestApplicationResponse().id(id));

        mockMvc.perform(get("/api/admin/guest-applications/{id}", id)
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void rejectsUpdateStatusWithoutTheManageAuthority() throws Exception {
        mockMvc.perform(patch("/api/admin/guest-applications/{id}/status", UUID.randomUUID())
                        .contentType("application/json").content("{\"status\":\"ACCEPTED\"}")
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_READ_OWN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsUpdateStatusWithTheManageAuthority() throws Exception {
        UUID id = UUID.randomUUID();
        given(guestApplicationService.updateStatus(any(), any())).willReturn(new GuestApplicationResponse().id(id));

        mockMvc.perform(patch("/api/admin/guest-applications/{id}/status", id)
                        .contentType("application/json").content("{\"status\":\"ACCEPTED\"}")
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    // ── public settings (authenticated, no specific authority) ─────────────

    @Test
    void rejectsPublicSettingsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/guest-application-settings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void allowsPublicSettingsWithAnyAuthenticatedToken() throws Exception {
        given(guestApplicationService.getPublicSettings())
                .willReturn(new PublicGuestApplicationSettingsResponse().enabled(true));

        mockMvc.perform(get("/api/guest-application-settings").with(jwt().authorities(() -> "FUNC_SOME_OTHER_PERMISSION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    // ── admin settings (FUNC_GUEST_APPLICATION_CONFIGURE) ───────────────────

    @Test
    void rejectsAdminSettingsGetWithoutTheConfigureAuthority() throws Exception {
        mockMvc.perform(get("/api/guest-application-settings/admin")
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_MANAGE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsAdminSettingsGetWithTheConfigureAuthority() throws Exception {
        given(guestApplicationService.getAdminSettings())
                .willReturn(new GuestApplicationSettingsResponse().enabled(true));

        mockMvc.perform(get("/api/guest-application-settings/admin")
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_CONFIGURE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void rejectsAdminSettingsUpdateWithoutTheConfigureAuthority() throws Exception {
        mockMvc.perform(put("/api/guest-application-settings/admin")
                        .contentType("application/json")
                        .content("{\"enabled\":false,\"recipientIds\":[]}")
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_MANAGE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsAdminSettingsUpdateWithTheConfigureAuthority() throws Exception {
        given(guestApplicationService.updateSettings(any()))
                .willReturn(new GuestApplicationSettingsResponse().enabled(true));

        mockMvc.perform(put("/api/guest-application-settings/admin")
                        .contentType("application/json")
                        .content("{\"enabled\":true,\"recipientIds\":[]}")
                        .with(jwt().authorities(() -> "FUNC_GUEST_APPLICATION_CONFIGURE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
