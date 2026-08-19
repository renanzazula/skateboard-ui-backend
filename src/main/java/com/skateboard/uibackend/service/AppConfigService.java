package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.AppConfigClient;
import com.skateboard.uibackend.client.appconfig.generated.model.BrandingAssetResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.BrandingConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeFeaturedPlayerConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeVideoCategoryConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateHomeFeaturedPlayerConfigRequest;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateLoginTextRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Thin pass-through today — same seam as {@link PodcastService}/{@link
 * UserService} for future orchestration/aggregation across multiple
 * downstream clients.
 */
@Service
public class AppConfigService {

    private final AppConfigClient appConfigClient;

    public AppConfigService(AppConfigClient appConfigClient) {
        this.appConfigClient = appConfigClient;
    }

    public PublicConfigResponse getPublicConfig() {
        return appConfigClient.getPublicConfig();
    }

    public BrandingConfigResponse getBrandingConfig() {
        return appConfigClient.getBrandingConfig();
    }

    public BrandingConfigResponse uploadLoginBackground(MultipartFile file) {
        return appConfigClient.uploadLoginBackground(file);
    }

    public BrandingConfigResponse removeLoginBackground() {
        return appConfigClient.removeLoginBackground();
    }

    public BrandingConfigResponse updateLoginText(UpdateLoginTextRequest request) {
        return appConfigClient.updateLoginText(request);
    }

    public BrandingConfigResponse uploadAppLogo(MultipartFile file) {
        return appConfigClient.uploadAppLogo(file);
    }

    public BrandingConfigResponse removeAppLogo() {
        return appConfigClient.removeAppLogo();
    }

    public List<BrandingAssetResponse> listBrandingAssets() {
        return appConfigClient.listBrandingAssets();
    }

    public BrandingAssetResponse uploadBrandingAsset(String name, MultipartFile file) {
        return appConfigClient.uploadBrandingAsset(name, file);
    }

    public BrandingAssetResponse replaceBrandingAsset(UUID assetId, MultipartFile file) {
        return appConfigClient.replaceBrandingAsset(assetId, file);
    }

    public void removeBrandingAsset(UUID assetId) {
        appConfigClient.removeBrandingAsset(assetId);
    }

    public HomeVideoCategoryConfigResponse getHomeVideoCategoryConfig() {
        return appConfigClient.getHomeVideoCategoryConfig();
    }

    public HomeVideoCategoryConfigResponse updateHomeVideoCategoryConfig(HomeVideoCategoryConfigRequest request) {
        return appConfigClient.updateHomeVideoCategoryConfig(request);
    }

    public HomeFeaturedPlayerConfigResponse getHomeFeaturedPlayerConfig() {
        return appConfigClient.getHomeFeaturedPlayerConfig();
    }

    public HomeFeaturedPlayerConfigResponse updateHomeFeaturedPlayerConfig(UpdateHomeFeaturedPlayerConfigRequest request) {
        return appConfigClient.updateHomeFeaturedPlayerConfig(request);
    }
}
