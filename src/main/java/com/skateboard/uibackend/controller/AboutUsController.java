package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.AboutImageResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.AboutPageResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateAboutPageRequest;
import com.skateboard.uibackend.service.AppConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Passes the frontend straight through to skateboard-app-config-be's
 * {@code /api/about-us} routes, mirroring {@link AppConfigController}.
 * <p>
 * {@code GET /api/about-us} backs the standard-user viewer, so it is gated by
 * {@code FUNC_TAB_SETTINGS} (the baseline authority every user holds) rather
 * than the admin-only {@code FUNC_ABOUT_US_MANAGE} the other three require. A
 * {@code null} from the service means app-config-be answered {@code 204} (no
 * page published/created yet) — relayed as {@code 204} here too.
 */
@RestController
public class AboutUsController {

    private final AppConfigService appConfigService;

    public AboutUsController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping("/api/about-us")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS')")
    public ResponseEntity<AboutPageResponse> getAboutUs() {
        AboutPageResponse page = appConfigService.getAboutUs();
        return page == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(page);
    }

    @GetMapping("/api/about-us/admin")
    @PreAuthorize("hasAuthority('FUNC_ABOUT_US_MANAGE')")
    public ResponseEntity<AboutPageResponse> getAboutUsAdmin() {
        AboutPageResponse page = appConfigService.getAboutUsAdmin();
        return page == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(page);
    }

    @PutMapping("/api/about-us")
    @PreAuthorize("hasAuthority('FUNC_ABOUT_US_MANAGE')")
    public AboutPageResponse updateAboutUs(@RequestBody UpdateAboutPageRequest request) {
        return appConfigService.updateAboutUs(request);
    }

    @PostMapping("/api/about-us/images")
    @PreAuthorize("hasAuthority('FUNC_ABOUT_US_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public AboutImageResponse uploadAboutUsImage(@RequestParam("file") MultipartFile file) {
        return appConfigService.uploadAboutUsImage(file);
    }
}
