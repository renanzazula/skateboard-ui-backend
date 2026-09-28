package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.notification.generated.model.DeviceResponse;
import com.skateboard.uibackend.client.notification.generated.model.InboxPageResponse;
import com.skateboard.uibackend.client.notification.generated.model.UnreadCountResponse;
import com.skateboard.uibackend.client.notification.generated.model.NotificationPreferencesResponse;
import com.skateboard.uibackend.client.notification.generated.model.TestNotificationResponse;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.NotificationService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void testNotificationRequiresAToken() throws Exception {
        mockMvc.perform(post("/api/me/notifications/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void testNotificationRejectsTheSelfServiceDeviceAuthorityAlone() throws Exception {
        mockMvc.perform(post("/api/me/notifications/test")
                        .with(jwt().authorities(() -> "FUNC_NOTIFICATION_DEVICE_MANAGE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void deviceManageTestAuthorityAllowsSendingATestNotification() throws Exception {
        given(notificationService.sendTestNotification())
                .willReturn(new TestNotificationResponse().devicesTargeted(1).sent(1));

        mockMvc.perform(post("/api/me/notifications/test")
                        .with(jwt().authorities(() -> "FUNC_NOTIFICATION_DEVICE_MANAGE_TEST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(1));
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

    // --- inbox: same FUNC_USER_SELF_READ / FUNC_USER_SELF_UPDATE as preferences ---

    @Test
    void inboxRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/me/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void inboxRequiresSelfRead() throws Exception {
        mockMvc.perform(get("/api/me/notifications").with(jwt().authorities(() -> "FUNC_TAB_HOME")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void selfReadAllowsListingTheInbox() throws Exception {
        given(notificationService.listInbox(1, 10)).willReturn(new InboxPageResponse().hasMore(false));

        mockMvc.perform(get("/api/me/notifications").param("page", "1").param("size", "10")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void selfReadAllowsTheUnreadCount() throws Exception {
        given(notificationService.getInboxUnreadCount()).willReturn(new UnreadCountResponse().count(6L));

        mockMvc.perform(get("/api/me/notifications/unread-count")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(6));
    }

    @Test
    void selfReadDoesNotAllowMarkingRead() throws Exception {
        mockMvc.perform(post("/api/me/notifications/" + UUID.randomUUID() + "/read")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_READ")))
                .andExpect(status().isForbidden());
    }

    @Test
    void selfUpdateAllowsMarkingOneRead() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/me/notifications/" + id + "/read")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_UPDATE")))
                .andExpect(status().isNoContent());

        verify(notificationService).markInboxNotificationRead(id);
    }

    @Test
    void aNonUuidNotificationIdIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/me/notifications/not-a-uuid/read")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_UPDATE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));
    }

    @Test
    void selfUpdateAllowsMarkingAllReadWithOrWithoutABody() throws Exception {
        mockMvc.perform(post("/api/me/notifications/read-all")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_UPDATE"))
                        .contentType("application/json")
                        .content("{\"before\":\"2026-09-28T10:00:00Z\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/me/notifications/read-all")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_UPDATE")))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllInboxNotificationsRead(isNull());
    }

    /** The literal /test route must still resolve to the admin diagnostic, not the inbox. */
    @Test
    void theTestRouteIsNotShadowedByTheInbox() throws Exception {
        mockMvc.perform(post("/api/me/notifications/test")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_UPDATE")))
                .andExpect(status().isForbidden());
    }
}
