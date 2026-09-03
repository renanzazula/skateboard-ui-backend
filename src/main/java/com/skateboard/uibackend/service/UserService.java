package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.user.UserClient;
import com.skateboard.uibackend.client.user.generated.model.ChangePasswordRequest;
import com.skateboard.uibackend.client.user.generated.model.ChangeUsernameRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportResponse;
import com.skateboard.uibackend.client.user.generated.model.UpdateUserRequest;
import com.skateboard.uibackend.client.user.generated.model.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Thin pass-through today — same seam as {@link PodcastService} for future
 * orchestration/aggregation across multiple downstream clients.
 */
@Service
public class UserService {

    private final UserClient userClient;

    public UserService(UserClient userClient) {
        this.userClient = userClient;
    }

    public UserResponse getCurrentUser() {
        return userClient.getCurrentUser();
    }

    public UserResponse updateCurrentUser(UpdateUserRequest request) {
        return userClient.updateCurrentUser(request);
    }

    public void deleteCurrentUser() {
        userClient.deleteCurrentUser();
    }

    public UserResponse uploadProfilePicture(MultipartFile file) {
        return userClient.uploadProfilePicture(file);
    }

    public UserResponse changeUsername(ChangeUsernameRequest request) {
        return userClient.changeUsername(request);
    }

    public void changePassword(ChangePasswordRequest request) {
        userClient.changePassword(request);
    }

    public UserResponse deactivateCurrentUser() {
        return userClient.deactivateCurrentUser();
    }

    public ProblemReportResponse reportProblem(ProblemReportRequest request) {
        return userClient.reportProblem(request);
    }
}
