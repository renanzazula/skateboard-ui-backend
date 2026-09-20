package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.AppConfigClient;
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
 * {@link AppConfigService} is a thin passthrough to {@link AppConfigClient} —
 * these tests just confirm each method delegates and returns the client's
 * result unchanged, mirroring the shape used for the other {@code *Service}
 * classes.
 */
class AppConfigServiceTest {

    @Mock
    private AppConfigClient appConfigClient;

    private AppConfigService service;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AppConfigService(appConfigClient);
    }

    @Test
    void getPublicConfigDelegates() {
        PublicConfigResponse response = new PublicConfigResponse();
        when(appConfigClient.getPublicConfig()).thenReturn(response);

        assertThat(service.getPublicConfig()).isSameAs(response);
    }

    @Test
    void getBrandingConfigDelegates() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigClient.getBrandingConfig()).thenReturn(response);

        assertThat(service.getBrandingConfig()).isSameAs(response);
    }

    @Test
    void uploadLoginBackgroundDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigClient.uploadLoginBackground(file)).thenReturn(response);

        assertThat(service.uploadLoginBackground(file)).isSameAs(response);
    }

    @Test
    void removeLoginBackgroundDelegates() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigClient.removeLoginBackground()).thenReturn(response);

        assertThat(service.removeLoginBackground()).isSameAs(response);
    }

    @Test
    void updateLoginTextDelegates() {
        UpdateLoginTextRequest request = new UpdateLoginTextRequest();
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigClient.updateLoginText(request)).thenReturn(response);

        assertThat(service.updateLoginText(request)).isSameAs(response);
    }

    @Test
    void uploadAppLogoDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigClient.uploadAppLogo(file)).thenReturn(response);

        assertThat(service.uploadAppLogo(file)).isSameAs(response);
    }

    @Test
    void removeAppLogoDelegates() {
        BrandingConfigResponse response = new BrandingConfigResponse();
        when(appConfigClient.removeAppLogo()).thenReturn(response);

        assertThat(service.removeAppLogo()).isSameAs(response);
    }

    @Test
    void listBrandingAssetsDelegates() {
        List<BrandingAssetResponse> response = List.of(new BrandingAssetResponse().name("logo"));
        when(appConfigClient.listBrandingAssets()).thenReturn(response);

        assertThat(service.listBrandingAssets()).isSameAs(response);
    }

    @Test
    void uploadBrandingAssetDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        BrandingAssetResponse response = new BrandingAssetResponse().name("logo");
        when(appConfigClient.uploadBrandingAsset("logo", file)).thenReturn(response);

        assertThat(service.uploadBrandingAsset("logo", file)).isSameAs(response);
    }

    @Test
    void replaceBrandingAssetDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        BrandingAssetResponse response = new BrandingAssetResponse().name("logo");
        when(appConfigClient.replaceBrandingAsset(id, file)).thenReturn(response);

        assertThat(service.replaceBrandingAsset(id, file)).isSameAs(response);
    }

    @Test
    void removeBrandingAssetDelegates() {
        service.removeBrandingAsset(id);

        verify(appConfigClient).removeBrandingAsset(id);
    }

    @Test
    void getHomeVideoCategoryConfigDelegates() {
        HomeVideoCategoryConfigResponse response = new HomeVideoCategoryConfigResponse();
        when(appConfigClient.getHomeVideoCategoryConfig()).thenReturn(response);

        assertThat(service.getHomeVideoCategoryConfig()).isSameAs(response);
    }

    @Test
    void updateHomeVideoCategoryConfigDelegates() {
        HomeVideoCategoryConfigRequest request = new HomeVideoCategoryConfigRequest();
        HomeVideoCategoryConfigResponse response = new HomeVideoCategoryConfigResponse();
        when(appConfigClient.updateHomeVideoCategoryConfig(request)).thenReturn(response);

        assertThat(service.updateHomeVideoCategoryConfig(request)).isSameAs(response);
    }

    @Test
    void getHomeFeaturedPlayerConfigDelegates() {
        HomeFeaturedPlayerConfigResponse response = new HomeFeaturedPlayerConfigResponse();
        when(appConfigClient.getHomeFeaturedPlayerConfig()).thenReturn(response);

        assertThat(service.getHomeFeaturedPlayerConfig()).isSameAs(response);
    }

    @Test
    void updateHomeFeaturedPlayerConfigDelegates() {
        UpdateHomeFeaturedPlayerConfigRequest request = new UpdateHomeFeaturedPlayerConfigRequest();
        HomeFeaturedPlayerConfigResponse response = new HomeFeaturedPlayerConfigResponse();
        when(appConfigClient.updateHomeFeaturedPlayerConfig(request)).thenReturn(response);

        assertThat(service.updateHomeFeaturedPlayerConfig(request)).isSameAs(response);
    }

    @Test
    void getAboutUsDelegates() {
        AboutPageResponse response = new AboutPageResponse();
        when(appConfigClient.getAboutUs()).thenReturn(response);

        assertThat(service.getAboutUs()).isSameAs(response);
    }

    @Test
    void getAboutUsAdminDelegates() {
        AboutPageResponse response = new AboutPageResponse();
        when(appConfigClient.getAboutUsAdmin()).thenReturn(response);

        assertThat(service.getAboutUsAdmin()).isSameAs(response);
    }

    @Test
    void updateAboutUsDelegates() {
        UpdateAboutPageRequest request = new UpdateAboutPageRequest();
        AboutPageResponse response = new AboutPageResponse();
        when(appConfigClient.updateAboutUs(request)).thenReturn(response);

        assertThat(service.updateAboutUs(request)).isSameAs(response);
    }

    @Test
    void uploadAboutUsImageDelegates() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        AboutImageResponse response = new AboutImageResponse().url("https://cdn/x.jpg");
        when(appConfigClient.uploadAboutUsImage(file)).thenReturn(response);

        assertThat(service.uploadAboutUsImage(file)).isSameAs(response);
    }
}
