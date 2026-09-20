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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Every {@link AppConfigClient} passthrough/upload method plus its shared
 * downstream-error mapping and per-operation message overrides (mirrors
 * {@link CampaignClientTest}'s shape).
 */
class AppConfigClientTest {

    @Mock
    private PublicApi publicApi;
    @Mock
    private AdminApi adminApi;
    @Mock
    private HomeApi homeApi;
    @Mock
    private HomeFeaturedPlayerApi homeFeaturedPlayerApi;
    @Mock
    private AboutUsApi aboutUsApi;

    private AppConfigClient client;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new AppConfigClient(publicApi, adminApi, homeApi, homeFeaturedPlayerApi, aboutUsApi);
    }

    private static WebClientResponseException responseException(HttpStatus status) {
        return WebClientResponseException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8, null);
    }

    @Test
    void getPublicConfigPassesThrough() {
        PublicConfigResponse response = new PublicConfigResponse();
        when(publicApi.getPublicConfig()).thenReturn(Mono.just(response));

        assertThat(client.getPublicConfig()).isSameAs(response);
    }

    @Test
    void getHomeVideoCategoryConfigPassesThrough() {
        HomeVideoCategoryConfigResponse response = new HomeVideoCategoryConfigResponse();
        when(homeApi.getHomeVideoCategoryConfig()).thenReturn(Mono.just(response));

        assertThat(client.getHomeVideoCategoryConfig()).isSameAs(response);
    }

    @Test
    void updateHomeVideoCategoryConfigPassesThrough() {
        HomeVideoCategoryConfigRequest request = new HomeVideoCategoryConfigRequest();
        HomeVideoCategoryConfigResponse response = new HomeVideoCategoryConfigResponse();
        when(homeApi.updateHomeVideoCategoryConfig(request)).thenReturn(Mono.just(response));

        assertThat(client.updateHomeVideoCategoryConfig(request)).isSameAs(response);
    }

    @Test
    void updateHomeVideoCategoryConfigMaps400ToItsDomainSpecificMessage() {
        when(homeApi.updateHomeVideoCategoryConfig(any())).thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.updateHomeVideoCategoryConfig(new HomeVideoCategoryConfigRequest()), DownstreamServiceException.class);

        assertThat(ex.getMessage()).contains("Select at least one category");
    }

    @Test
    void getHomeFeaturedPlayerConfigPassesThrough() {
        HomeFeaturedPlayerConfigResponse response = new HomeFeaturedPlayerConfigResponse();
        when(homeFeaturedPlayerApi.getHomeFeaturedPlayerConfig()).thenReturn(Mono.just(response));

        assertThat(client.getHomeFeaturedPlayerConfig()).isSameAs(response);
    }

    @Test
    void updateHomeFeaturedPlayerConfigPassesThrough() {
        UpdateHomeFeaturedPlayerConfigRequest request = new UpdateHomeFeaturedPlayerConfigRequest();
        HomeFeaturedPlayerConfigResponse response = new HomeFeaturedPlayerConfigResponse();
        when(homeFeaturedPlayerApi.updateHomeFeaturedPlayerConfig(request)).thenReturn(Mono.just(response));

        assertThat(client.updateHomeFeaturedPlayerConfig(request)).isSameAs(response);
    }

    @Test
    void updateHomeFeaturedPlayerConfigMaps400ToItsDomainSpecificMessage() {
        when(homeFeaturedPlayerApi.updateHomeFeaturedPlayerConfig(any()))
                .thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.updateHomeFeaturedPlayerConfig(new UpdateHomeFeaturedPlayerConfigRequest()), DownstreamServiceException.class);

        assertThat(ex.getMessage()).contains("Select a source and content");
    }

    @Test
    void getBrandingConfigPassesThrough() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(adminApi.getBrandingConfig()).thenReturn(Mono.just(response));

        assertThat(client.getBrandingConfig()).isSameAs(response);
    }

    @Test
    void uploadLoginBackgroundMaterializesATempFile() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(adminApi.uploadLoginBackground(any())).thenReturn(Mono.just(response));
        MockMultipartFile file = new MockMultipartFile("file", "bg.png", "image/png", new byte[] {1, 2, 3});

        assertThat(client.uploadLoginBackground(file)).isSameAs(response);
    }

    @Test
    void removeLoginBackgroundPassesThrough() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(adminApi.removeLoginBackground()).thenReturn(Mono.just(response));

        assertThat(client.removeLoginBackground()).isSameAs(response);
    }

    @Test
    void updateLoginTextPassesThrough() {
        UpdateLoginTextRequest request = new UpdateLoginTextRequest();
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(adminApi.updateLoginText(request)).thenReturn(Mono.just(response));

        assertThat(client.updateLoginText(request)).isSameAs(response);
    }

    @Test
    void uploadAppLogoMaterializesATempFileWithoutAnOriginalFilename() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(adminApi.uploadAppLogo(any())).thenReturn(Mono.just(response));
        MockMultipartFile file = new MockMultipartFile("file", null, "image/png", new byte[] {1, 2, 3});

        assertThat(client.uploadAppLogo(file)).isSameAs(response);
    }

    @Test
    void removeAppLogoPassesThrough() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(adminApi.removeAppLogo()).thenReturn(Mono.just(response));

        assertThat(client.removeAppLogo()).isSameAs(response);
    }

    @Test
    void listBrandingAssetsPassesThroughTheWholeList() {
        List<BrandingAssetResponse> assets = List.of(new BrandingAssetResponse().name("logo"));
        when(adminApi.listBrandingAssets()).thenReturn(Flux.fromIterable(assets));

        assertThat(client.listBrandingAssets()).containsExactlyElementsOf(assets);
    }

    @Test
    void uploadBrandingAssetMaterializesATempFile() {
        BrandingAssetResponse response = new BrandingAssetResponse().name("logo");
        when(adminApi.uploadBrandingAsset(any(), any())).thenReturn(Mono.just(response));
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1, 2, 3});

        assertThat(client.uploadBrandingAsset("logo", file)).isSameAs(response);
    }

    @Test
    void replaceBrandingAssetMaterializesATempFile() {
        BrandingAssetResponse response = new BrandingAssetResponse().name("logo");
        when(adminApi.replaceBrandingAsset(any(), any())).thenReturn(Mono.just(response));
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1, 2, 3});

        assertThat(client.replaceBrandingAsset(id, file)).isSameAs(response);
    }

    @Test
    void removeBrandingAssetDelegatesAndBlocks() {
        when(adminApi.removeBrandingAsset(id)).thenReturn(Mono.empty());

        client.removeBrandingAsset(id);
    }

    @Test
    void getAboutUsPassesThrough() {
        AboutPageResponse response = new AboutPageResponse();
        when(aboutUsApi.getAboutUs()).thenReturn(Mono.just(response));

        assertThat(client.getAboutUs()).isSameAs(response);
    }

    @Test
    void getAboutUsAdminPassesThrough() {
        AboutPageResponse response = new AboutPageResponse();
        when(aboutUsApi.getAboutUsAdmin()).thenReturn(Mono.just(response));

        assertThat(client.getAboutUsAdmin()).isSameAs(response);
    }

    @Test
    void updateAboutUsPassesThrough() {
        UpdateAboutPageRequest request = new UpdateAboutPageRequest();
        AboutPageResponse response = new AboutPageResponse();
        when(aboutUsApi.updateAboutUs(request)).thenReturn(Mono.just(response));

        assertThat(client.updateAboutUs(request)).isSameAs(response);
    }

    @Test
    void updateAboutUsMaps400ToItsDomainSpecificMessage() {
        when(aboutUsApi.updateAboutUs(any())).thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST)));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.updateAboutUs(new UpdateAboutPageRequest()), DownstreamServiceException.class);

        assertThat(ex.getMessage()).contains("could not be saved");
    }

    @Test
    void uploadAboutUsImageMaterializesATempFile() {
        AboutImageResponse response = new AboutImageResponse().url("https://cdn/x.jpg");
        when(aboutUsApi.uploadAboutUsImage(any())).thenReturn(Mono.just(response));
        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[] {1, 2, 3});

        assertThat(client.uploadAboutUsImage(file)).isSameAs(response);
    }

    @Test
    void uploadAboutUsImageMaps400ToItsDomainSpecificMessage() {
        when(aboutUsApi.uploadAboutUsImage(any())).thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST)));
        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[] {1, 2, 3});

        DownstreamServiceException ex = catchThrowableOfType(() -> client.uploadAboutUsImage(file), DownstreamServiceException.class);

        assertThat(ex.getMessage()).contains("could not be saved");
    }

    @Test
    void maps404ToNotFound() {
        when(adminApi.getBrandingConfig()).thenReturn(Mono.error(responseException(HttpStatus.NOT_FOUND)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getBrandingConfig(), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("Branding asset not found");
    }

    @Test
    void maps400ToBadRequest() {
        when(adminApi.getBrandingConfig()).thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getBrandingConfig(), DownstreamServiceException.class);

        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_BAD_REQUEST");
        assertThat(ex.getMessage()).isEqualTo("Invalid branding request");
    }

    @Test
    void maps409ToConflict() {
        when(adminApi.getBrandingConfig()).thenReturn(Mono.error(responseException(HttpStatus.CONFLICT)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getBrandingConfig(), DownstreamServiceException.class);

        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_CONFLICT");
        assertThat(ex.getMessage()).isEqualTo("A branding asset with this name already exists");
    }

    @Test
    void mapsAnUnrecognizedStatusToTheGenericRequestErrorCode() {
        when(adminApi.getBrandingConfig()).thenReturn(Mono.error(responseException(HttpStatus.UNPROCESSABLE_ENTITY)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getBrandingConfig(), DownstreamServiceException.class);

        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_REQUEST_ERROR");
        assertThat(ex.getMessage()).isEqualTo("App config service rejected the request");
    }

    @Test
    void maps5xxToServiceUnavailable() {
        when(adminApi.getBrandingConfig()).thenReturn(Mono.error(responseException(HttpStatus.BAD_GATEWAY)));

        DownstreamServiceException ex = catchThrowableOfType(() -> client.getBrandingConfig(), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_SERVICE_UNAVAILABLE");
    }

    @Test
    void mapsAConnectivityFailureOnAListCallToServiceUnavailable() {
        when(adminApi.listBrandingAssets()).thenReturn(Flux.error(new WebClientRequestException(
                new RuntimeException("connection refused"), HttpMethod.GET,
                URI.create("http://app-config-be/api/branding/assets"), HttpHeaders.EMPTY)));

        assertThatThrownBy(() -> client.listBrandingAssets())
                .isInstanceOf(DownstreamServiceException.class)
                .satisfies(t -> assertThat(((DownstreamServiceException) t).getCode())
                        .isEqualTo("APP_CONFIG_SERVICE_UNAVAILABLE"));
    }

    @Test
    void mapsAConnectivityFailureOnASingleCallToServiceUnavailable() {
        when(publicApi.getPublicConfig()).thenReturn(Mono.error(new WebClientRequestException(
                new RuntimeException("connection refused"), HttpMethod.GET,
                URI.create("http://app-config-be/api/config"), HttpHeaders.EMPTY)));

        assertThatThrownBy(() -> client.getPublicConfig())
                .isInstanceOf(DownstreamServiceException.class)
                .satisfies(t -> assertThat(((DownstreamServiceException) t).getCode())
                        .isEqualTo("APP_CONFIG_SERVICE_UNAVAILABLE"));
    }
}
