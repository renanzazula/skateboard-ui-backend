package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.user.generated.model.ChangePasswordRequest;
import com.skateboard.uibackend.client.user.generated.model.ChangeUsernameRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportResponse;
import com.skateboard.uibackend.client.user.generated.model.UpdateUserRequest;
import com.skateboard.uibackend.client.user.generated.model.UserResponse;
import com.skateboard.uibackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserController} just delegates to {@link UserService} — mirrors
 * {@link CampaignControllerTest}'s shape; auth enforcement is covered
 * separately by {@link UserControllerSecurityTest}.
 */
class UserControllerTest {

    @Mock
    private UserService userService;

    private UserController controller;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new UserController(userService);
    }

    @Test
    void getCurrentUserDelegates() {
        UserResponse response = new UserResponse().id(id);
        when(userService.getCurrentUser()).thenReturn(response);

        assertThat(controller.getCurrentUser()).isSameAs(response);
    }

    @Test
    void updateCurrentUserDelegates() {
        UpdateUserRequest request = new UpdateUserRequest();
        UserResponse response = new UserResponse().id(id);
        when(userService.updateCurrentUser(request)).thenReturn(response);

        assertThat(controller.updateCurrentUser(request)).isSameAs(response);
    }

    @Test
    void deleteCurrentUserDelegates() {
        controller.deleteCurrentUser();

        verify(userService).deleteCurrentUser();
    }

    @Test
    void uploadProfilePictureDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        UserResponse response = new UserResponse().id(id);
        when(userService.uploadProfilePicture(file)).thenReturn(response);

        assertThat(controller.uploadProfilePicture(file)).isSameAs(response);
    }

    @Test
    void changeUsernameDelegates() {
        ChangeUsernameRequest request = new ChangeUsernameRequest();
        UserResponse response = new UserResponse().id(id);
        when(userService.changeUsername(request)).thenReturn(response);

        assertThat(controller.changeUsername(request)).isSameAs(response);
    }

    @Test
    void changePasswordDelegates() {
        ChangePasswordRequest request = new ChangePasswordRequest();

        controller.changePassword(request);

        verify(userService).changePassword(request);
    }

    @Test
    void deactivateCurrentUserDelegates() {
        UserResponse response = new UserResponse().id(id);
        when(userService.deactivateCurrentUser()).thenReturn(response);

        assertThat(controller.deactivateCurrentUser()).isSameAs(response);
    }

    @Test
    void reportProblemDelegates() {
        ProblemReportRequest request = new ProblemReportRequest();
        ProblemReportResponse response = new ProblemReportResponse();
        when(userService.reportProblem(request)).thenReturn(response);

        assertThat(controller.reportProblem(request)).isSameAs(response);
    }
}
