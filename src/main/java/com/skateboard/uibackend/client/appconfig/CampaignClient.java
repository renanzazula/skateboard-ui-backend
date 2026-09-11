package com.skateboard.uibackend.client.appconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skateboard.uibackend.client.appconfig.generated.api.CampaignApi;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignEventRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignRuntimeResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignScreenResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.ReorderCampaignScreensRequest;
import com.skateboard.uibackend.client.support.TempFiles;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wraps the generated {@link CampaignApi}, mirroring {@link AppConfigClient}'s
 * blocking-call and exception-mapping shape (see {@link
 * com.skateboard.uibackend.client.podcast.PodcastClient} for the general
 * rationale). Kept separate from {@link AppConfigClient} because the campaign
 * surface is large and self-contained (spec §15) — same reasoning as the
 * per-feature split on the controller/service side.
 * <p>
 * {@code uploadCampaignScreenImage} needs the temp-file materialization {@link
 * com.skateboard.uibackend.client.user.UserClient#uploadProfilePicture} uses
 * (see its javadoc): the openapi-generator java/webclient library maps a
 * {@code multipart/form-data} binary field to {@code java.io.File}, not
 * {@code MultipartFile}/{@code Resource}.
 */
@Component
public class CampaignClient {

    private final CampaignApi campaignApi;

    public CampaignClient(CampaignApi campaignApi) {
        this.campaignApi = campaignApi;
    }

    // ── Runtime (anonymous) ───────────────────────────────────────────────

    public List<CampaignRuntimeResponse> getActiveCampaigns() {
        return callList(campaignApi::getActiveCampaigns);
    }

    public void recordCampaignEvent(UUID campaignId, CampaignEventRequest request) {
        call(() -> campaignApi.recordCampaignEvent(campaignId, request), CampaignClient::eventMessageFor);
    }

    // ── Admin ────────────────────────────────────────────────────────────

    public List<CampaignResponse> listCampaigns() {
        return callList(campaignApi::listCampaigns);
    }

    public CampaignResponse getCampaign(UUID campaignId) {
        return call(() -> campaignApi.getCampaign(campaignId));
    }

    public CampaignResponse createCampaign(CampaignRequest request) {
        return call(() -> campaignApi.createCampaign(request), CampaignClient::validationMessageFor);
    }

    public CampaignResponse updateCampaign(UUID campaignId, CampaignRequest request) {
        return call(() -> campaignApi.updateCampaign(campaignId, request), CampaignClient::validationMessageFor);
    }

    public void deleteCampaign(UUID campaignId) {
        call(() -> campaignApi.deleteCampaign(campaignId), CampaignClient::lifecycleMessageFor);
    }

    public CampaignResponse publishCampaign(UUID campaignId) {
        return call(() -> campaignApi.publishCampaign(campaignId), CampaignClient::lifecycleMessageFor);
    }

    public CampaignResponse pauseCampaign(UUID campaignId) {
        return call(() -> campaignApi.pauseCampaign(campaignId), CampaignClient::lifecycleMessageFor);
    }

    public CampaignResponse archiveCampaign(UUID campaignId) {
        return call(() -> campaignApi.archiveCampaign(campaignId), CampaignClient::lifecycleMessageFor);
    }

    public CampaignScreenResponse addCampaignScreen(UUID campaignId, CampaignScreenRequest request) {
        return call(() -> campaignApi.addCampaignScreen(campaignId, request), CampaignClient::validationMessageFor);
    }

    public CampaignResponse reorderCampaignScreens(UUID campaignId, ReorderCampaignScreensRequest request) {
        return call(() -> campaignApi.reorderCampaignScreens(campaignId, request), CampaignClient::validationMessageFor);
    }

    public CampaignScreenResponse updateCampaignScreen(UUID campaignId, UUID screenId, CampaignScreenRequest request) {
        return call(() -> campaignApi.updateCampaignScreen(campaignId, screenId, request), CampaignClient::validationMessageFor);
    }

    public void removeCampaignScreen(UUID campaignId, UUID screenId) {
        call(() -> campaignApi.removeCampaignScreen(campaignId, screenId));
    }

    public CampaignScreenResponse uploadCampaignScreenImage(UUID campaignId, UUID screenId, MultipartFile file,
                                                            Float focalPointX, Float focalPointY) {
        Path tempFile = toTempFile(file, "campaign-screen-");
        try {
            return call(() -> campaignApi.uploadCampaignScreenImage(campaignId, screenId, tempFile.toFile(),
                    focalPointX, focalPointY), CampaignClient::validationMessageFor);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    // ── call plumbing (mirrors AppConfigClient) ──────────────────────────

    private <T> T call(Supplier<Mono<T>> invocation) {
        return call(invocation, CampaignClient::messageFor);
    }

    private <T> T call(Supplier<Mono<T>> invocation, Function<HttpStatusCode, String> messageResolver) {
        try {
            return invocation.get().block();
        } catch (WebClientResponseException ex) {
            throw mapResponseException(ex, messageResolver);
        } catch (WebClientRequestException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private <T> List<T> callList(Supplier<Flux<T>> invocation) {
        try {
            return invocation.get().collectList().block();
        } catch (WebClientResponseException ex) {
            throw mapResponseException(ex, CampaignClient::messageFor);
        } catch (WebClientRequestException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private DownstreamServiceException mapResponseException(WebClientResponseException ex,
                                                           Function<HttpStatusCode, String> messageResolver) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.is5xxServerError()) {
            return serviceUnavailable(ex);
        }
        // Prefer app-config-be's own error message ("Screen 1 needs a background
        // image or a fallback colour…") — the campaign domain messages are
        // specific and admin-actionable, unlike branding/About Us where the
        // canned text is fine. Fall back to the per-verb resolver when the
        // downstream body isn't a parseable {message: …}.
        String message = downstreamMessage(ex).orElseGet(() -> messageResolver.apply(status));
        return new DownstreamServiceException(status, codeFor(status), message, ex);
    }

    private Optional<String> downstreamMessage(WebClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode message = OBJECT_MAPPER.readTree(body).get("message");
            if (message != null && message.isTextual() && !message.asText().isBlank()) {
                return Optional.of(message.asText());
            }
        } catch (IOException ignored) {
            // not JSON, or no "message" field — use the canned fallback
        }
        return Optional.empty();
    }

    private DownstreamServiceException serviceUnavailable(Throwable cause) {
        return new DownstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "APP_CONFIG_SERVICE_UNAVAILABLE",
                "App config service is currently unavailable", cause);
    }

    private static String codeFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "APP_CONFIG_NOT_FOUND";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "APP_CONFIG_BAD_REQUEST";
        }
        if (status.equals(HttpStatus.CONFLICT)) {
            return "APP_CONFIG_CONFLICT";
        }
        return "APP_CONFIG_REQUEST_ERROR";
    }

    private static String messageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "Campaign or screen not found";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Invalid campaign request";
        }
        if (status.equals(HttpStatus.CONFLICT)) {
            return "This campaign is not in a state that allows this change";
        }
        return "App config service rejected the request";
    }

    // create/update/screen edits reject with 400 on a broken invariant
    // (screen limit, duration cap, close-delay, missing action target).
    private static String validationMessageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "The campaign could not be saved — check the schedule, screens and screen limits.";
        }
        return messageFor(status);
    }

    // publish/pause/archive/delete reject with 409 when the transition isn't
    // legal from the campaign's current status (e.g. deleting a published
    // campaign, publishing one with no screens).
    private static String lifecycleMessageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.CONFLICT)) {
            return "This campaign can't move to that state from its current status.";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "The campaign doesn't meet the requirements for this change (needs 1–3 screens, total ≤ 10s, endAt after startAt).";
        }
        return messageFor(status);
    }

    // recordCampaignEvent is best-effort analytics — downstream only ever
    // rejects with 400 on a malformed body; everything else uses messageFor.
    private static String eventMessageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Invalid campaign event payload";
        }
        return messageFor(status);
    }

    private Path toTempFile(MultipartFile file, String prefix) {
        try {
            String suffix = file.getOriginalFilename() != null
                    ? "-" + file.getOriginalFilename().replaceAll("[/\\\\]", "_")
                    : null;
            Path tempFile = TempFiles.createSecureTempFile(prefix, suffix);
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
