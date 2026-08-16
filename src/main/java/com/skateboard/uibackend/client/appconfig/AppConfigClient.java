package com.skateboard.uibackend.client.appconfig;

import com.skateboard.uibackend.client.appconfig.generated.api.AdminApi;
import com.skateboard.uibackend.client.appconfig.generated.api.PublicApi;
import com.skateboard.uibackend.client.appconfig.generated.model.BrandingAssetResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.BrandingConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicConfigResponse;
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

    public AppConfigClient(PublicApi publicApi, AdminApi adminApi) {
        this.publicApi = publicApi;
        this.adminApi = adminApi;
    }

    public PublicConfigResponse getPublicConfig() {
        return call(publicApi::getPublicConfig);
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

    private <T> T call(Supplier<Mono<T>> invocation) {
        try {
            return invocation.get().block();
        } catch (WebClientResponseException ex) {
            throw mapResponseException(ex);
        } catch (WebClientRequestException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private <T> List<T> callList(Supplier<Flux<T>> invocation) {
        try {
            return invocation.get().collectList().block();
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
