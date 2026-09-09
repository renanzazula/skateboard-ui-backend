package com.skateboard.uibackend.client.appconfig;

import com.skateboard.uibackend.client.appconfig.generated.api.CampaignApi;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignEventRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRuntimeResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenResponse;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

/**
 * The BFF's downstream-error mapping for campaigns — the one piece of
 * {@link CampaignClient} with real logic. Success is a pass-through; failures
 * become {@link DownstreamServiceException} carrying the status, an
 * {@code APP_CONFIG_*} code, and — unlike the other app-config clients —
 * app-config-be's own error message when it sent one.
 */
class CampaignClientTest {

    @Mock
    private CampaignApi campaignApi;

    private CampaignClient client;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new CampaignClient(campaignApi);
    }

    private static WebClientResponseException responseException(HttpStatus status, String body) {
        return WebClientResponseException.create(
                status, status.getReasonPhrase(), HttpHeaders.EMPTY,
                body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, null);
    }

    @Test
    void passesASuccessfulResponseThrough() {
        CampaignResponse response = new CampaignResponse().id(id).name("Launch week");
        when(campaignApi.getCampaign(id)).thenReturn(Mono.just(response));

        assertThat(client.getCampaign(id)).isSameAs(response);
    }

    @Test
    void maps404ToNotFoundWithTheDownstreamMessage() {
        when(campaignApi.getCampaign(id)).thenReturn(Mono.error(responseException(
                HttpStatus.NOT_FOUND, "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Campaign not found: " + id + "\"}")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.getCampaign(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("Campaign not found: " + id);
    }

    @Test
    void maps409ToConflictWithTheDomainInvariantMessage() {
        when(campaignApi.publishCampaign(id)).thenReturn(Mono.error(responseException(
                HttpStatus.CONFLICT,
                "{\"message\":\"Screen 1 needs a background image or a fallback colour before publishing.\"}")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.publishCampaign(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_CONFLICT");
        assertThat(ex.getMessage())
                .isEqualTo("Screen 1 needs a background image or a fallback colour before publishing.");
    }

    @Test
    void fallsBackToTheCannedMessageWhenTheBodyIsNotParseable() {
        when(campaignApi.createCampaign(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST, "<html>gateway error</html>")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.createCampaign(null), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_BAD_REQUEST");
        assertThat(ex.getMessage()).contains("could not be saved");
    }

    @Test
    void maps5xxToServiceUnavailable() {
        when(campaignApi.getCampaign(id)).thenReturn(Mono.error(responseException(
                HttpStatus.INTERNAL_SERVER_ERROR, "{\"message\":\"boom\"}")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.getCampaign(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_SERVICE_UNAVAILABLE");
    }

    @Test
    void mapsAConnectivityFailureToServiceUnavailable() {
        when(campaignApi.listCampaigns()).thenReturn(reactor.core.publisher.Flux.error(
                new WebClientRequestException(new RuntimeException("connection refused"),
                        org.springframework.http.HttpMethod.GET, URI.create("http://app-config-be/api/campaigns/admin"),
                        HttpHeaders.EMPTY)));

        assertThatThrownBy(() -> client.listCampaigns())
                .isInstanceOf(DownstreamServiceException.class)
                .satisfies(t -> assertThat(((DownstreamServiceException) t).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    // ── the runtime routes ────────────────────────────────────────────────

    @Test
    void collectsTheActiveCampaignFluxIntoAList() {
        CampaignRuntimeResponse first = new CampaignRuntimeResponse().id(id).priority(10);
        CampaignRuntimeResponse second = new CampaignRuntimeResponse().id(UUID.randomUUID()).priority(5);
        when(campaignApi.getActiveCampaigns()).thenReturn(reactor.core.publisher.Flux.just(first, second));

        assertThat(client.getActiveCampaigns()).containsExactly(first, second);
    }

    @Test
    void anEmptyActiveCampaignFluxBecomesAnEmptyListNotNull() {
        when(campaignApi.getActiveCampaigns()).thenReturn(reactor.core.publisher.Flux.empty());

        assertThat(client.getActiveCampaigns()).isEmpty();
    }

    @Test
    void aRejectedEventReportsTheEventSpecificCannedMessage() {
        CampaignEventRequest request = new CampaignEventRequest();
        when(campaignApi.recordCampaignEvent(id, request))
                .thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST, "not json")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.recordCampaignEvent(id, request), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("Invalid campaign event payload");
    }

    // ── per-verb canned fallbacks ─────────────────────────────────────────

    @Test
    void anUnparseableLifecycleConflictFallsBackToTheTransitionMessage() {
        when(campaignApi.archiveCampaign(id))
                .thenReturn(Mono.error(responseException(HttpStatus.CONFLICT, "")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.archiveCampaign(id), DownstreamServiceException.class);

        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_CONFLICT");
        assertThat(ex.getMessage()).isEqualTo("This campaign can't move to that state from its current status.");
    }

    @Test
    void anUnparseableLifecycleBadRequestExplainsTheAggregateRequirements() {
        when(campaignApi.publishCampaign(id))
                .thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST, null)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.publishCampaign(id), DownstreamServiceException.class);

        assertThat(ex.getMessage()).contains("1–3 screens").contains("10s");
    }

    @Test
    void aBodyWhoseMessageIsBlankFallsBackRatherThanRelayingEmptyText() {
        when(campaignApi.getCampaign(id)).thenReturn(Mono.error(responseException(
                HttpStatus.NOT_FOUND, "{\"message\":\"   \"}")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.getCampaign(id), DownstreamServiceException.class);

        assertThat(ex.getMessage()).isEqualTo("Campaign or screen not found");
    }

    @Test
    void anUnmappedClientErrorGetsTheGenericCodeAndMessage() {
        when(campaignApi.getCampaign(id)).thenReturn(Mono.error(
                responseException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.getCampaign(id), DownstreamServiceException.class);

        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_REQUEST_ERROR");
        assertThat(ex.getMessage()).isEqualTo("App config service rejected the request");
    }

    // ── the multipart upload materializes a temp file ─────────────────────

    @Test
    void theUploadTempFileIsDeletedOnceTheCallSucceeds() {
        MockMultipartFile file = new MockMultipartFile("file", "hero.webp", "image/webp", "bytes".getBytes());
        List<File> seen = new ArrayList<>();
        when(campaignApi.uploadCampaignScreenImage(org.mockito.ArgumentMatchers.eq(id),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> {
                    seen.add(inv.getArgument(2));
                    return Mono.just(new CampaignScreenResponse());
                });

        client.uploadCampaignScreenImage(id, UUID.randomUUID(), file, 0.5f, 0.5f);

        assertThat(seen).hasSize(1);
        assertThat(seen.get(0)).doesNotExist();
    }

    @Test
    void theUploadTempFileIsDeletedEvenWhenTheDownstreamCallFails() {
        MockMultipartFile file = new MockMultipartFile("file", "hero.webp", "image/webp", "bytes".getBytes());
        List<File> seen = new ArrayList<>();
        when(campaignApi.uploadCampaignScreenImage(org.mockito.ArgumentMatchers.eq(id),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> {
                    seen.add(inv.getArgument(2));
                    return Mono.error(responseException(HttpStatus.BAD_REQUEST, ""));
                });

        assertThatThrownBy(() -> client.uploadCampaignScreenImage(id, UUID.randomUUID(), file, null, null))
                .isInstanceOf(DownstreamServiceException.class);

        assertThat(seen).hasSize(1);
        assertThat(seen.get(0)).doesNotExist();
    }
}
