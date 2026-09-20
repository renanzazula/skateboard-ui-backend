package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.user.UserClient;
import com.skateboard.uibackend.client.user.generated.model.ChangePasswordRequest;
import com.skateboard.uibackend.client.user.generated.model.ChangeUsernameRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportResponse;
import com.skateboard.uibackend.client.user.generated.model.UpdateUserRequest;
import com.skateboard.uibackend.client.user.generated.model.UserResponse;
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
 * {@link UserService} is a thin passthrough to {@link UserClient} — mirrors
 * {@link AppConfigServiceTest}'s shape.
 */
class UserServiceTest {

    @Mock
    private UserClient userClient;

    private UserService service;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserService(userClient);
    }

    @Test
    void getCurrentUserDelegates() {
        UserResponse response = new UserResponse().id(id);
        when(userClient.getCurrentUser()).thenReturn(response);

        assertThat(service.getCurrentUser()).isSameAs(response);
    }

    @Test
    void updateCurrentUserDelegates() {
        UpdateUserRequest request = new UpdateUserRequest();
        UserResponse response = new UserResponse().id(id);
        when(userClient.updateCurrentUser(request)).thenReturn(response);

        assertThat(service.updateCurrentUser(request)).isSameAs(response);
    }

    @Test
    void deleteCurrentUserDelegates() {
        service.deleteCurrentUser();

        verify(userClient).deleteCurrentUser();
    }

    @Test
    void uploadProfilePictureDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        UserResponse response = new UserResponse().id(id);
        when(userClient.uploadProfilePicture(file)).thenReturn(response);

        assertThat(service.uploadProfilePicture(file)).isSameAs(response);
    }

    @Test
    void changeUsernameDelegates() {
        ChangeUsernameRequest request = new ChangeUsernameRequest();
        UserResponse response = new UserResponse().id(id);
        when(userClient.changeUsername(request)).thenReturn(response);

        assertThat(service.changeUsername(request)).isSameAs(response);
    }

    @Test
    void changePasswordDelegates() {
        ChangePasswordRequest request = new ChangePasswordRequest();

        service.changePassword(request);

        verify(userClient).changePassword(request);
    }

    @Test
    void deactivateCurrentUserDelegates() {
        UserResponse response = new UserResponse().id(id);
        when(userClient.deactivateCurrentUser()).thenReturn(response);

        assertThat(service.deactivateCurrentUser()).isSameAs(response);
    }

    @Test
    void reportProblemDelegates() {
        ProblemReportRequest request = new ProblemReportRequest();
        ProblemReportResponse response = new ProblemReportResponse();
        when(userClient.reportProblem(request)).thenReturn(response);

        assertThat(service.reportProblem(request)).isSameAs(response);
    }
}
