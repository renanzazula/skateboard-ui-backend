package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.notification.generated.model.NotificationPreferences;
import com.skateboard.uibackend.client.notification.generated.model.NotificationPreferencesResponse;
import com.skateboard.uibackend.client.notification.generated.model.UpdateNotificationPreferencesRequest;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.NotificationService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The podcast-notification setting travels through this BFF untouched:
 * {@code PATCH /api/me/preferences} must hand the exact {@code newPodcastEnabled}
 * value to the notification service, and {@code GET} must return whatever that
 * service reports (including the enabled default for a user with no saved
 * preference) without altering it. The BFF holds no preference state of its own.
 */
@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class NotificationPreferencesPassThroughTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private NotificationService notificationService;

    @Test
    void getReturnsTheDownstreamPodcastPreferenceAsIs() throws Exception {
        given(notificationService.getNotificationPreferences()).willReturn(
                new NotificationPreferencesResponse().notifications(
                        new NotificationPreferences().pushEnabled(true).newPodcastEnabled(false)));

        mockMvc.perform(get("/api/me/preferences").with(jwt().authorities(() -> "FUNC_USER_SELF_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.newPodcastEnabled").value(false))
                .andExpect(jsonPath("$.notifications.pushEnabled").value(true));
    }

    @Test
    void getReturnsEnabledWhenDownstreamReportsTheDefault() throws Exception {
        given(notificationService.getNotificationPreferences()).willReturn(
                new NotificationPreferencesResponse().notifications(
                        new NotificationPreferences().pushEnabled(true).newPodcastEnabled(true)));

        mockMvc.perform(get("/api/me/preferences").with(jwt().authorities(() -> "FUNC_USER_SELF_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.newPodcastEnabled").value(true));
    }

    @Test
    void patchForwardsOnlyTheSuppliedPodcastFlagDownstream() throws Exception {
        given(notificationService.updateNotificationPreferences(any())).willReturn(
                new NotificationPreferencesResponse().notifications(
                        new NotificationPreferences().newPodcastEnabled(false)));

        mockMvc.perform(patch("/api/me/preferences")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_UPDATE"))
                        .contentType("application/json")
                        .content("{\"notifications\":{\"newPodcastEnabled\":false}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.newPodcastEnabled").value(false));

        ArgumentCaptor<UpdateNotificationPreferencesRequest> captor =
                ArgumentCaptor.forClass(UpdateNotificationPreferencesRequest.class);
        verify(notificationService).updateNotificationPreferences(captor.capture());
        assertThat(captor.getValue().getNotifications().getNewPodcastEnabled()).isFalse();
        // Untouched fields stay null so downstream leaves them unchanged (other notification
        // controls are not affected by toggling podcasts).
        assertThat(captor.getValue().getNotifications().getPushEnabled()).isNull();
    }

    @Test
    void patchCanReEnablePodcastNotifications() throws Exception {
        given(notificationService.updateNotificationPreferences(any())).willReturn(
                new NotificationPreferencesResponse().notifications(
                        new NotificationPreferences().newPodcastEnabled(true)));

        mockMvc.perform(patch("/api/me/preferences")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_UPDATE"))
                        .contentType("application/json")
                        .content("{\"notifications\":{\"newPodcastEnabled\":true}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.newPodcastEnabled").value(true));

        ArgumentCaptor<UpdateNotificationPreferencesRequest> captor =
                ArgumentCaptor.forClass(UpdateNotificationPreferencesRequest.class);
        verify(notificationService).updateNotificationPreferences(captor.capture());
        assertThat(captor.getValue().getNotifications().getNewPodcastEnabled()).isTrue();
    }
}
