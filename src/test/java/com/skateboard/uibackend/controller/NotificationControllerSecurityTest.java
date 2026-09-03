package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.notification.generated.model.DeviceResponse;
import com.skateboard.uibackend.client.notification.generated.model.NotificationPreferencesResponse;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.NotificationService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies this BFF's own authentication/authorization gate for the
 * notification routes, against the authorities api/notification-openapi.yaml
 * declares as x-required-permissions — not skateboard-notification-be's own
 * checks, which run downstream against the relayed token. Mirrors
 * {@link PodcastControllerSecurityTest}.
 *
 * <p>The two /api/me/preferences cases moved here from
 * {@link UserControllerSecurityTest} along with the route. That they still
 * expect FUNC_USER_SELF_READ and FUNC_USER_SELF_UPDATE is the point: the
 * contract did not change when the service behind it did, so the realm needs
 * no new role and the mobile settings screen needs no change.
 */
@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class NotificationControllerSecurityTest {

    private static final String REGISTER_BODY = """
            {"platform":"IOS","provider":"EXPO","pushToken":"ExponentPushToken[abc]","appVersion":"1.5.0"}""";

    @Autowired private MockMvc mockMvc;

    @MockBean private NotificationService notificationService;

    @Test
    void rejectsRequestsWithoutAToken() throws Exception {
        mockMvc.perform(put("/api/me/devices/install-1")
                        .contentType("application/json").content(REGISTER_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsATokenMissingTheRequiredAuthority() throws Exception {
        mockMvc.perform(put("/api/me/devices/install-1")
                        .with(jwt().authorities(() -> "FUNC_SOME_OTHER_PERMISSION"))
                        .contentType("application/json").content(REGISTER_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void deviceManageAuthorityAllowsRegisteringADevice() throws Exception {
        given(notificationService.registerDevice(anyString(), any())).willReturn(new DeviceResponse());

        mockMvc.perform(put("/api/me/devices/install-1")
                        .with(jwt().authorities(() -> "FUNC_NOTIFICATION_DEVICE_MANAGE"))
                        .contentType("application/json").content(REGISTER_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void deviceManageAuthorityAllowsRemovingADevice() throws Exception {
        mockMvc.perform(delete("/api/me/devices/install-1")
                        .with(jwt().authorities(() -> "FUNC_NOTIFICATION_DEVICE_MANAGE")))
                .andExpect(status().isNoContent());
    }

    @Test
    void selfReadAuthorityStillAllowsGetNotificationPreferences() throws Exception {
        given(notificationService.getNotificationPreferences())
                .willReturn(new NotificationPreferencesResponse());

        mockMvc.perform(get("/api/me/preferences").with(jwt().authorities(() -> "FUNC_USER_SELF_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void selfUpdateAuthorityStillAllowsUpdateNotificationPreferences() throws Exception {
        given(notificationService.updateNotificationPreferences(any()))
                .willReturn(new NotificationPreferencesResponse());

        mockMvc.perform(patch("/api/me/preferences")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_UPDATE"))
                        .contentType("application/json")
                        .content("{\"notifications\":{\"newPodcastEnabled\":false}}"))
                .andExpect(status().isOk());
    }

    /** Reading preferences is not licence to change them. */
    @Test
    void selfReadAuthorityDoesNotAllowUpdatingPreferences() throws Exception {
        mockMvc.perform(patch("/api/me/preferences")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_READ"))
                        .contentType("application/json")
                        .content("{\"notifications\":{\"newPodcastEnabled\":false}}"))
                .andExpect(status().isForbidden());
    }

    /**
     * Registering a device is a distinct permission from self-service profile
     * access, so a token that can read the profile must not be able to add a
     * push destination.
     */
    @Test
    void selfReadAuthorityDoesNotAllowRegisteringADevice() throws Exception {
        mockMvc.perform(put("/api/me/devices/install-1")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_READ"))
                        .contentType("application/json").content(REGISTER_BODY))
                .andExpect(status().isForbidden());
    }
}
