package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.BrandingAssetResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.BrandingConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.PublicConfigResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateLoginTextRequest;
import com.skateboard.uibackend.service.AppConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Passes the frontend straight through to skateboard-app-config-be — same
 * paths, same request/response DTOs, mirroring {@link PodcastController}.
 * {@code GET /api/config} is the one public (pre-auth) route in this
 * controller — {@code app-config-openapi.yaml}'s "public" tag carries no
 * {@code x-required-permissions}, and {@code SecurityConfig} explicitly
 * permits it. Every other route mirrors the "admin" tag's
 * {@code x-required-permissions: [FUNC_TAB_SETTINGS_BRANDING]}.
 */
@RestController
public class AppConfigController {

    private final AppConfigService appConfigService;

    public AppConfigController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping("/api/config")
    public PublicConfigResponse getPublicConfig() {
        return appConfigService.getPublicConfig();
    }

    @GetMapping("/api/config/branding")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS_BRANDING')")
    public BrandingConfigResponse getBrandingConfig() {
        return appConfigService.getBrandingConfig();
    }

    @PostMapping("/api/config/branding/login-background")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS_BRANDING')")
    public BrandingConfigResponse uploadLoginBackground(@RequestParam("file") MultipartFile file) {
        return appConfigService.uploadLoginBackground(file);
    }

    @DeleteMapping("/api/config/branding/login-background")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS_BRANDING')")
    public BrandingConfigResponse removeLoginBackground() {
        return appConfigService.removeLoginBackground();
    }

    @PutMapping("/api/config/branding/login-text")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS_BRANDING')")
    public BrandingConfigResponse updateLoginText(@RequestBody UpdateLoginTextRequest request) {
        return appConfigService.updateLoginText(request);
    }

    @PostMapping("/api/config/branding/app-logo")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS_BRANDING')")
    public BrandingConfigResponse uploadAppLogo(@RequestParam("file") MultipartFile file) {
        return appConfigService.uploadAppLogo(file);
    }

    @DeleteMapping("/api/config/branding/app-logo")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS_BRANDING')")
    public BrandingConfigResponse removeAppLogo() {
        return appConfigService.removeAppLogo();
    }

    @GetMapping("/api/config/branding/assets")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS_BRANDING')")
    public List<BrandingAssetResponse> listBrandingAssets() {
        return appConfigService.listBrandingAssets();
    }

    @PostMapping("/api/config/branding/assets")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS_BRANDING')")
    @ResponseStatus(HttpStatus.CREATED)
    public BrandingAssetResponse uploadBrandingAsset(@RequestParam("name") String name,
                                                       @RequestParam("file") MultipartFile file) {
        return appConfigService.uploadBrandingAsset(name, file);
    }

    @PutMapping("/api/config/branding/assets/{assetId}")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS_BRANDING')")
    public BrandingAssetResponse replaceBrandingAsset(@PathVariable UUID assetId,
                                                        @RequestParam("file") MultipartFile file) {
        return appConfigService.replaceBrandingAsset(assetId, file);
    }

    @DeleteMapping("/api/config/branding/assets/{assetId}")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS_BRANDING')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBrandingAsset(@PathVariable UUID assetId) {
        appConfigService.removeBrandingAsset(assetId);
    }
}
