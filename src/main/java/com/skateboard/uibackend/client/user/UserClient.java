package com.skateboard.uibackend.client.user;

import com.skateboard.uibackend.client.user.generated.api.MeApi;
import com.skateboard.uibackend.client.user.generated.model.ChangePasswordRequest;
import com.skateboard.uibackend.client.user.generated.model.ChangeUsernameRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportResponse;
import com.skateboard.uibackend.client.user.generated.model.UpdateUserRequest;
import com.skateboard.uibackend.client.user.generated.model.UserResponse;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Wraps the generated {@link MeApi}, mirroring {@link
 * com.skateboard.uibackend.client.podcast.PodcastClient}'s blocking-call and
 * exception-mapping shape (see its javadoc for the general rationale).
 * <p>
 * The one difference from the podcast client: the openapi-generator
 * java/webclient library maps a {@code multipart/form-data} binary field to
 * {@code java.io.File} (not {@code MultipartFile}/{@code Resource}), and
 * builds the multipart body part as {@code new FileSystemResource(file)}
 * internally — so the inbound {@link MultipartFile} has to be materialized
 * to a temp file before calling {@link MeApi#uploadProfilePicture}, and
 * cleaned up afterward regardless of outcome.
 */
@Component
public class UserClient {

    private final MeApi meApi;

    public UserClient(MeApi meApi) {
        this.meApi = meApi;
    }

    public UserResponse getCurrentUser() {
        return call(meApi::getCurrentUser);
    }

    public UserResponse updateCurrentUser(UpdateUserRequest request) {
        return call(() -> meApi.updateCurrentUser(request));
    }

    public void deleteCurrentUser() {
        call(meApi::deleteCurrentUser);
    }

    public UserResponse uploadProfilePicture(MultipartFile file) {
        Path tempFile = toTempFile(file);
        try {
            return call(() -> meApi.uploadProfilePicture(tempFile.toFile()));
        } finally {
            deleteQuietly(tempFile);
        }
    }

    public UserResponse changeUsername(ChangeUsernameRequest request) {
        return call(() -> meApi.changeUsername(request));
    }

    public void changePassword(ChangePasswordRequest request) {
        call(() -> meApi.changePassword(request));
    }

    public UserResponse deactivateCurrentUser() {
        return call(meApi::deactivateCurrentUser);
    }

    public ProblemReportResponse reportProblem(ProblemReportRequest request) {
        return call(() -> meApi.reportProblem(request));
    }

    private <T> T call(Supplier<Mono<T>> invocation) {
        try {
            return invocation.get().block();
        } catch (WebClientResponseException ex) {
            throw mapResponseException(ex);
        } catch (WebClientRequestException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private DownstreamServiceException mapResponseException(WebClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.is5xxServerError()) {
            return serviceUnavailable(ex);
        }
        return new DownstreamServiceException(status, codeFor(status), messageFor(status), ex);
    }

    private DownstreamServiceException serviceUnavailable(Throwable cause) {
        return new DownstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "USER_SERVICE_UNAVAILABLE",
                "User service is currently unavailable", cause);
    }

    private static String codeFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "USER_NOT_FOUND";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "USER_BAD_REQUEST";
        }
        return "USER_REQUEST_ERROR";
    }

    private static String messageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "User not found";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Invalid user request";
        }
        return "User service rejected the request";
    }

    private Path toTempFile(MultipartFile file) {
        try {
            String suffix = file.getOriginalFilename() != null
                    ? "-" + file.getOriginalFilename().replaceAll("[/\\\\]", "_")
                    : null;
            Path tempFile = Files.createTempFile("profile-picture-", suffix);
            file.transferTo(tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void deleteQuietly(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
            // best-effort cleanup of a temp file; the OS temp-dir reaper is the backstop
        }
    }
}
