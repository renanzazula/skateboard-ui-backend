package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.user.generated.model.ProblemReportResponse;
import com.skateboard.uibackend.client.user.generated.model.UserResponse;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.UserService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the BFF's own authentication/authorization gate for every one of
 * the six FUNC_USER_* authorities copied from api/user-openapi.yaml's
 * x-required-permissions — not skateboard-user-be's, which is a separate
 * service exercised via UserClient (mocked out here through UserService).
 * Mirrors {@link PodcastControllerSecurityTest}.
 */
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void rejectsRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsATokenMissingTheRequiredAuthority() throws Exception {
        mockMvc.perform(get("/api/me").with(jwt().authorities(() -> "FUNC_SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void selfReadAuthorityAllowsGetCurrentUser() throws Exception {
        given(userService.getCurrentUser()).willReturn(new UserResponse());

        mockMvc.perform(get("/api/me").with(jwt().authorities(() -> "FUNC_USER_SELF_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void selfUpdateAuthorityAllowsUpdateCurrentUser() throws Exception {
        given(userService.updateCurrentUser(any())).willReturn(new UserResponse());

        mockMvc.perform(patch("/api/me")
                        .with(jwt().authorities(() -> "FUNC_USER_SELF_UPDATE"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void accountDeleteAuthorityAllowsDeleteCurrentUser() throws Exception {
        mockMvc.perform(delete("/api/me").with(jwt().authorities(() -> "FUNC_USER_ACCOUNT_DELETE")))
                .andExpect(status().isNoContent());
    }

    @Test
    void passwordChangeAuthorityAllowsChangePassword() throws Exception {
        mockMvc.perform(post("/api/me/change-password")
                        .with(jwt().authorities(() -> "FUNC_USER_PASSWORD_CHANGE"))
                        .contentType("application/json")
                        .content("{\"newPassword\":\"a-new-password\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void accountDeactivateAuthorityAllowsDeactivateCurrentUser() throws Exception {
        given(userService.deactivateCurrentUser()).willReturn(new UserResponse());

        mockMvc.perform(post("/api/me/deactivate").with(jwt().authorities(() -> "FUNC_USER_ACCOUNT_DEACTIVATE")))
                .andExpect(status().isOk());
    }

    @Test
    void problemReportCreateAuthorityAllowsReportProblem() throws Exception {
        given(userService.reportProblem(any())).willReturn(new ProblemReportResponse());

        mockMvc.perform(post("/api/me/problem-reports")
                        .with(jwt().authorities(() -> "FUNC_USER_PROBLEM_REPORT_CREATE"))
                        .contentType("application/json")
                        .content("{\"category\":\"APP_ERROR\",\"message\":\"it broke\"}"))
                .andExpect(status().isCreated());
    }
}
