package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.BrandingAssetResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.BrandingConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeFeaturedPlayerConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateHomeFeaturedPlayerConfigRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateLoginTextRequest;
import com.skateboard.uibackend.service.AppConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AppConfigController} just delegates to {@link AppConfigService} —
 * mirrors {@link CampaignControllerTest}'s shape; auth enforcement is covered
 * separately by the {@code *SecurityTest} suites.
 */
class AppConfigControllerTest {

    @Mock
    private AppConfigService appConfigService;

    private AppConfigController controller;

    private final UUID assetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new AppConfigController(appConfigService);
    }

    @Test
    void getPublicConfigDelegates() {
        PublicConfigResponse response = new PublicConfigResponse();
        when(appConfigService.getPublicConfig()).thenReturn(response);

        assertThat(controller.getPublicConfig()).isSameAs(response);
    }

    @Test
    void getBrandingConfigDelegates() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigService.getBrandingConfig()).thenReturn(response);

        assertThat(controller.getBrandingConfig()).isSameAs(response);
    }

    @Test
    void uploadLoginBackgroundDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigService.uploadLoginBackground(file)).thenReturn(response);

        assertThat(controller.uploadLoginBackground(file)).isSameAs(response);
    }

    @Test
    void removeLoginBackgroundDelegates() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigService.removeLoginBackground()).thenReturn(response);

        assertThat(controller.removeLoginBackground()).isSameAs(response);
    }

    @Test
    void updateLoginTextDelegates() {
        UpdateLoginTextRequest request = new UpdateLoginTextRequest();
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigService.updateLoginText(request)).thenReturn(response);

        assertThat(controller.updateLoginText(request)).isSameAs(response);
    }

    @Test
    void uploadAppLogoDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigService.uploadAppLogo(file)).thenReturn(response);

        assertThat(controller.uploadAppLogo(file)).isSameAs(response);
    }

    @Test
    void removeAppLogoDelegates() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigService.removeAppLogo()).thenReturn(response);

        assertThat(controller.removeAppLogo()).isSameAs(response);
    }

    @Test
    void listBrandingAssetsDelegates() {
        List<BrandingAssetResponse> response = List.of(new BrandingAssetResponse().name("logo"));
        when(appConfigService.listBrandingAssets()).thenReturn(response);

        assertThat(controller.listBrandingAssets()).isSameAs(response);
    }

    @Test
    void uploadBrandingAssetDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        BrandingAssetResponse response = new BrandingAssetResponse().name("logo");
        when(appConfigService.uploadBrandingAsset("logo", file)).thenReturn(response);

        assertThat(controller.uploadBrandingAsset("logo", file)).isSameAs(response);
    }

    @Test
    void replaceBrandingAssetDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        BrandingAssetResponse response = new BrandingAssetResponse().name("logo");
        when(appConfigService.replaceBrandingAsset(assetId, file)).thenReturn(response);

        assertThat(controller.replaceBrandingAsset(assetId, file)).isSameAs(response);
    }

    @Test
    void removeBrandingAssetDelegates() {
        controller.removeBrandingAsset(assetId);

        verify(appConfigService).removeBrandingAsset(assetId);
    }

    @Test
    void getHomeVideoCategoryConfigDelegates() {
        HomeVideoCategoryConfigResponse response = new HomeVideoCategoryConfigResponse();
        when(appConfigService.getHomeVideoCategoryConfig()).thenReturn(response);

        assertThat(controller.getHomeVideoCategoryConfig()).isSameAs(response);
    }

    @Test
    void updateHomeVideoCategoryConfigDelegates() {
        HomeVideoCategoryConfigRequest request = new HomeVideoCategoryConfigRequest();
        HomeVideoCategoryConfigResponse response = new HomeVideoCategoryConfigResponse();
        when(appConfigService.updateHomeVideoCategoryConfig(request)).thenReturn(response);

        assertThat(controller.updateHomeVideoCategoryConfig(request)).isSameAs(response);
    }

    @Test
    void getHomeFeaturedPlayerConfigDelegates() {
        HomeFeaturedPlayerConfigResponse response = new HomeFeaturedPlayerConfigResponse();
        when(appConfigService.getHomeFeaturedPlayerConfig()).thenReturn(response);

        assertThat(controller.getHomeFeaturedPlayerConfig()).isSameAs(response);
    }

    @Test
    void updateHomeFeaturedPlayerConfigDelegates() {
        UpdateHomeFeaturedPlayerConfigRequest request = new UpdateHomeFeaturedPlayerConfigRequest();
        HomeFeaturedPlayerConfigResponse response = new HomeFeaturedPlayerConfigResponse();
        when(appConfigService.updateHomeFeaturedPlayerConfig(request)).thenReturn(response);

        assertThat(controller.updateHomeFeaturedPlayerConfig(request)).isSameAs(response);
    }
}
