package com.skateboard.uibackend.client.user;

import com.skateboard.uibackend.client.user.generated.api.MeApi;
import com.skateboard.uibackend.client.user.generated.model.ChangePasswordRequest;
import com.skateboard.uibackend.client.user.generated.model.ChangeUsernameRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportRequest;
import com.skateboard.uibackend.client.user.generated.model.ProblemReportResponse;
import com.skateboard.uibackend.client.user.generated.model.UpdateUserRequest;
import com.skateboard.uibackend.client.user.generated.model.UserResponse;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Every {@link UserClient} passthrough method, its downstream-error mapping
 * (mirrors {@link com.skateboard.uibackend.client.appconfig.CampaignClientTest}),
 * and the upload-profile-picture temp-file materialization.
 */
class UserClientTest {

    @Mock
    private MeApi meApi;

    private UserClient client;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new UserClient(meApi);
    }

    private static WebClientResponseException responseException(HttpStatus status) {
        return WebClientResponseException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8, null);
    }

    @Test
    void getCurrentUserPassesThrough() {
        UserResponse response = new UserResponse().id(id);
        when(meApi.getCurrentUser()).thenReturn(Mono.just(response));

        assertThat(client.getCurrentUser()).isSameAs(response);
    }

    @Test
    void updateCurrentUserPassesThrough() {
        UpdateUserRequest request = new UpdateUserRequest();
        UserResponse response = new UserResponse().id(id);
        when(meApi.updateCurrentUser(request)).thenReturn(Mono.just(response));

        assertThat(client.updateCurrentUser(request)).isSameAs(response);
    }

    @Test
    void deleteCurrentUserDelegatesAndBlocks() {
        when(meApi.deleteCurrentUser()).thenReturn(Mono.empty());

        client.deleteCurrentUser();
    }

    @Test
    void uploadProfilePictureMaterializesATempFileAndCleansItUp() throws Exception {
        UserResponse response = new UserResponse().id(id);
        when(meApi.uploadProfilePicture(any())).thenReturn(Mono.just(response));
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});

        assertThat(client.uploadProfilePicture(file)).isSameAs(response);
    }

    @Test
    void uploadProfilePictureWorksWithoutAnOriginalFilename() {
        UserResponse response = new UserResponse().id(id);
        when(meApi.uploadProfilePicture(any())).thenReturn(Mono.just(response));
        MockMultipartFile file = new MockMultipartFile("file", null, "image/png", new byte[] {1, 2, 3});

        assertThat(client.uploadProfilePicture(file)).isSameAs(response);
    }

    @Test
    void changeUsernamePassesThrough() {
        ChangeUsernameRequest request = new ChangeUsernameRequest();
        UserResponse response = new UserResponse().id(id);
        when(meApi.changeUsername(request)).thenReturn(Mono.just(response));

        assertThat(client.changeUsername(request)).isSameAs(response);
    }

    @Test
    void changePasswordDelegatesAndBlocks() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        when(meApi.changePassword(request)).thenReturn(Mono.empty());

        client.changePassword(request);
    }

    @Test
    void deactivateCurrentUserPassesThrough() {
        UserResponse response = new UserResponse().id(id);
        when(meApi.deactivateCurrentUser()).thenReturn(Mono.just(response));

        assertThat(client.deactivateCurrentUser()).isSameAs(response);
    }

    @Test
    void reportProblemPassesThrough() {
        ProblemReportRequest request = new ProblemReportRequest();
        ProblemReportResponse response = new ProblemReportResponse();
        when(meApi.reportProblem(request)).thenReturn(Mono.just(response));

        assertThat(client.reportProblem(request)).isSameAs(response);
    }

    @Test
    void maps404ToNotFound() {
        when(meApi.getCurrentUser()).thenReturn(Mono.error(responseException(HttpStatus.NOT_FOUND)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getCurrentUser(), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("User not found");
    }

    @Test
    void maps400ToBadRequest() {
        when(meApi.updateCurrentUser(any())).thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.updateCurrentUser(new UpdateUserRequest()), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("USER_BAD_REQUEST");
        assertThat(ex.getMessage()).isEqualTo("Invalid user request");
    }

    @Test
    void mapsAnUnrecognizedStatusToTheGenericRequestErrorCode() {
        when(meApi.getCurrentUser()).thenReturn(Mono.error(responseException(HttpStatus.CONFLICT)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getCurrentUser(), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getCode()).isEqualTo("USER_REQUEST_ERROR");
        assertThat(ex.getMessage()).isEqualTo("User service rejected the request");
    }

    @Test
    void maps5xxToServiceUnavailable() {
        when(meApi.getCurrentUser()).thenReturn(Mono.error(responseException(HttpStatus.BAD_GATEWAY)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getCurrentUser(), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getCode()).isEqualTo("USER_SERVICE_UNAVAILABLE");
    }

    @Test
    void mapsAConnectivityFailureToServiceUnavailable() {
        when(meApi.getCurrentUser()).thenReturn(Mono.error(new WebClientRequestException(
                new RuntimeException("connection refused"), HttpMethod.GET,
                URI.create("http://user-be/api/me"), HttpHeaders.EMPTY)));

        assertThatThrownBy(() -> client.getCurrentUser())
                .isInstanceOf(DownstreamServiceException.class)
                .satisfies(t -> assertThat(((DownstreamServiceException) t).getCode())
                        .isEqualTo("USER_SERVICE_UNAVAILABLE"));
    }
}
