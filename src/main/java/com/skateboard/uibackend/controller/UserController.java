package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.user.generated.model.ChangePasswordRequest;
import com.skateboard.uibackend.client.user.generated.model.ChangeUsernameRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportResponse;
import com.skateboard.uibackend.client.user.generated.model.UpdateUserRequest;
import com.skateboard.uibackend.client.user.generated.model.UserResponse;
import com.skateboard.uibackend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Exposes skateboard-user-be's self-service API under {@code /api/me/**} —
 * the BFF-facing prefix every other endpoint here uses (see
 * PodcastController), even though skateboard-user-be's own paths have no
 * {@code /api} prefix (its generated client's paths come straight from
 * api/user-openapi.yaml regardless of what this controller exposes). The
 * {@code @PreAuthorize} authorities are copied from api/user-openapi.yaml's
 * {@code x-required-permissions} — coarse-grained, token-claim-only checks at
 * this API boundary, not a duplicate of skateboard-user-be's own
 * per-operation authorization, which still runs downstream against the
 * relayed token.
 *
 * <p>{@code /api/me/preferences} is deliberately absent: notification
 * preferences moved to skateboard-notification-be and are served by
 * {@link NotificationController}, on the same route and with the same
 * authorities. skateboard-user-be still exposes its own copy, which nothing
 * calls any more.
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/me")
    @PreAuthorize("hasAuthority('FUNC_USER_SELF_READ')")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @PatchMapping("/api/me")
    @PreAuthorize("hasAuthority('FUNC_USER_SELF_UPDATE')")
    public UserResponse updateCurrentUser(@RequestBody UpdateUserRequest request) {
        return userService.updateCurrentUser(request);
    }

    @DeleteMapping("/api/me")
    @PreAuthorize("hasAuthority('FUNC_USER_ACCOUNT_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrentUser() {
        userService.deleteCurrentUser();
    }

    @PostMapping("/api/me/profile-picture")
    @PreAuthorize("hasAuthority('FUNC_USER_SELF_UPDATE')")
    public UserResponse uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        return userService.uploadProfilePicture(file);
    }

    @PostMapping("/api/me/username")
    @PreAuthorize("hasAuthority('FUNC_USER_SELF_UPDATE')")
    public UserResponse changeUsername(@RequestBody ChangeUsernameRequest request) {
        return userService.changeUsername(request);
    }

    @PostMapping("/api/me/change-password")
    @PreAuthorize("hasAuthority('FUNC_USER_PASSWORD_CHANGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
    }

    @PostMapping("/api/me/deactivate")
    @PreAuthorize("hasAuthority('FUNC_USER_ACCOUNT_DEACTIVATE')")
    public UserResponse deactivateCurrentUser() {
        return userService.deactivateCurrentUser();
    }

    @PostMapping("/api/me/problem-reports")
    @PreAuthorize("hasAuthority('FUNC_USER_PROBLEM_REPORT_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemReportResponse reportProblem(@RequestBody ProblemReportRequest request) {
        return userService.reportProblem(request);
    }
}
