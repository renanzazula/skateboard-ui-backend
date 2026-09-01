package com.skateboard.uibackend.client.appconfig;

import com.skateboard.uibackend.client.appconfig.generated.api.AboutUsApi;
import com.skateboard.uibackend.client.appconfig.generated.api.AdminApi;
import com.skateboard.uibackend.client.appconfig.generated.api.HomeApi;
import com.skateboard.uibackend.client.appconfig.generated.api.HomeFeaturedPlayerApi;
import com.skateboard.uibackend.client.appconfig.generated.api.PublicApi;
import com.skateboard.uibackend.client.appconfig.generated.model.AboutImageResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.AboutPageResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.BrandingAssetResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.BrandingConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeFeaturedPlayerConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateAboutPageRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateHomeFeaturedPlayerConfigRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateLoginTextRequest;
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
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wraps the generated {@link PublicApi}/{@link AdminApi}, mirroring {@link
 * com.skateboard.uibackend.client.podcast.PodcastClient}'s blocking-call and
 * exception-mapping shape (see its javadoc for the general rationale).
 * <p>
 * Every upload/replace endpoint here needs the same temp-file materialization
 * {@link com.skateboard.uibackend.client.user.UserClient#uploadProfilePicture}
 * uses (see its javadoc): the openapi-generator java/webclient library maps a
 * {@code multipart/form-data} binary field to {@code java.io.File}, not
 * {@code MultipartFile}/{@code Resource}.
 */
@Component
public class AppConfigClient {

    private final PublicApi publicApi;
    private final AdminApi adminApi;
    private final HomeApi homeApi;
    private final HomeFeaturedPlayerApi homeFeaturedPlayerApi;
    private final AboutUsApi aboutUsApi;

    public AppConfigClient(PublicApi publicApi, AdminApi adminApi, HomeApi homeApi,
                           HomeFeaturedPlayerApi homeFeaturedPlayerApi, AboutUsApi aboutUsApi) {
        this.publicApi = publicApi;
        this.adminApi = adminApi;
        this.homeApi = homeApi;
        this.homeFeaturedPlayerApi = homeFeaturedPlayerApi;
        this.aboutUsApi = aboutUsApi;
    }

    public PublicConfigResponse getPublicConfig() {
        return call(publicApi::getPublicConfig);
    }

    public HomeVideoCategoryConfigResponse getHomeVideoCategoryConfig() {
        return call(homeApi::getHomeVideoCategoryConfig);
    }

    public HomeVideoCategoryConfigResponse updateHomeVideoCategoryConfig(HomeVideoCategoryConfigRequest request) {
        return call(() -> homeApi.updateHomeVideoCategoryConfig(request), AppConfigClient::homeMessageFor);
    }

    public HomeFeaturedPlayerConfigResponse getHomeFeaturedPlayerConfig() {
        return call(homeFeaturedPlayerApi::getHomeFeaturedPlayerConfig);
    }

    public HomeFeaturedPlayerConfigResponse updateHomeFeaturedPlayerConfig(UpdateHomeFeaturedPlayerConfigRequest request) {
        return call(() -> homeFeaturedPlayerApi.updateHomeFeaturedPlayerConfig(request), AppConfigClient::featuredPlayerMessageFor);
    }

    public BrandingConfigResponse getBrandingConfig() {
        return call(adminApi::getBrandingConfig);
    }

    public BrandingConfigResponse uploadLoginBackground(MultipartFile file) {
        Path tempFile = toTempFile(file, "login-background-");
        try {
            return call(() -> adminApi.uploadLoginBackground(tempFile.toFile()));
        } finally {
            deleteQuietly(tempFile);
        }
    }

    public BrandingConfigResponse removeLoginBackground() {
        return call(adminApi::removeLoginBackground);
    }

    public BrandingConfigResponse updateLoginText(UpdateLoginTextRequest request) {
        return call(() -> adminApi.updateLoginText(request));
    }

    public BrandingConfigResponse uploadAppLogo(MultipartFile file) {
        Path tempFile = toTempFile(file, "app-logo-");
        try {
            return call(() -> adminApi.uploadAppLogo(tempFile.toFile()));
        } finally {
            deleteQuietly(tempFile);
        }
    }

    public BrandingConfigResponse removeAppLogo() {
        return call(adminApi::removeAppLogo);
    }

    public List<BrandingAssetResponse> listBrandingAssets() {
        return callList(adminApi::listBrandingAssets);
    }

    public BrandingAssetResponse uploadBrandingAsset(String name, MultipartFile file) {
        Path tempFile = toTempFile(file, "branding-asset-");
        try {
            return call(() -> adminApi.uploadBrandingAsset(name, tempFile.toFile()));
        } finally {
            deleteQuietly(tempFile);
        }
    }

    public BrandingAssetResponse replaceBrandingAsset(UUID assetId, MultipartFile file) {
        Path tempFile = toTempFile(file, "branding-asset-");
        try {
            return call(() -> adminApi.replaceBrandingAsset(assetId, tempFile.toFile()));
        } finally {
            deleteQuietly(tempFile);
        }
    }

    public void removeBrandingAsset(UUID assetId) {
        call(() -> adminApi.removeBrandingAsset(assetId));
    }

    // ── About Us ──────────────────────────────────────────────────────────
    // getAboutUs / getAboutUsAdmin return null on a downstream 204 (empty
    // Mono): the app-config service answers 204 when there is nothing to show,
    // and AboutUsController turns that null back into a 204 for the frontend.

    public AboutPageResponse getAboutUs() {
        return call(aboutUsApi::getAboutUs);
    }

    public AboutPageResponse getAboutUsAdmin() {
        return call(aboutUsApi::getAboutUsAdmin);
    }

    public AboutPageResponse updateAboutUs(UpdateAboutPageRequest request) {
        return call(() -> aboutUsApi.updateAboutUs(request), AppConfigClient::aboutUsMessageFor);
    }

    public AboutImageResponse uploadAboutUsImage(MultipartFile file) {
        Path tempFile = toTempFile(file, "about-us-image-");
        try {
            return call(() -> aboutUsApi.uploadAboutUsImage(tempFile.toFile()), AppConfigClient::aboutUsMessageFor);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    private <T> T call(Supplier<Mono<T>> invocation) {
        return call(invocation, AppConfigClient::messageFor);
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
            throw mapResponseException(ex, AppConfigClient::messageFor);
        } catch (WebClientRequestException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private DownstreamServiceException mapResponseException(WebClientResponseException ex, Function<HttpStatusCode, String> messageResolver) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.is5xxServerError()) {
            return serviceUnavailable(ex);
        }
        return new DownstreamServiceException(status, codeFor(status), messageResolver.apply(status), ex);
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
            return "Branding asset not found";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Invalid branding request";
        }
        if (status.equals(HttpStatus.CONFLICT)) {
            return "A branding asset with this name already exists";
        }
        return "App config service rejected the request";
    }

    // Home category config's only reachable error status is 400 (invalid
    // mode/selection) — everything else falls back to messageFor's generic
    // text rather than duplicating it.
    private static String homeMessageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Select at least one category, or choose \"All categories\".";
        }
        return messageFor(status);
    }

    // Featured Player config's only reachable error status is 400 (enabled
    // without a selected content source/id) — everything else falls back to
    // messageFor's generic text, mirroring homeMessageFor.
    private static String featuredPlayerMessageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Select a source and content, or turn the Featured Player off.";
        }
        return messageFor(status);
    }

    // About Us save/image-upload reject with 400 on a blank title or an
    // unsupported/oversized image — everything else falls back to messageFor.
    private static String aboutUsMessageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "The About Us page could not be saved — check the title and section content.";
        }
        return messageFor(status);
    }

    private Path toTempFile(MultipartFile file, String prefix) {
        try {
            String suffix = file.getOriginalFilename() != null
                    ? "-" + file.getOriginalFilename().replaceAll("[/\\\\]", "_")
                    : null;
            Path tempFile = Files.createTempFile(prefix, suffix);
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
