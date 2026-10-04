package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.LicensesResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateLicensesRequest;
import com.skateboard.uibackend.service.AppConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Passes the frontend straight through to skateboard-app-config-be's
 * {@code /api/licenses} routes, mirroring {@link AboutUsController}.
 * <p>
 * {@code GET /api/licenses} backs the standard-user viewer, so it is gated by
 * {@code FUNC_TAB_SETTINGS} (the baseline authority every user holds) rather
 * than the admin-only {@code FUNC_LICENSES_MANAGE} the other two require. A
 * {@code null} from the service means app-config-be answered {@code 204} (no
 * page published/created yet) — relayed as {@code 204} here too.
 */
@RestController
public class LicensesController {

    private final AppConfigService appConfigService;

    public LicensesController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping("/api/licenses")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS')")
    public ResponseEntity<LicensesResponse> getLicenses() {
        LicensesResponse page = appConfigService.getLicenses();
        return page == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(page);
    }

    @GetMapping("/api/licenses/admin")
    @PreAuthorize("hasAuthority('FUNC_LICENSES_MANAGE')")
    public ResponseEntity<LicensesResponse> getLicensesAdmin() {
        LicensesResponse page = appConfigService.getLicensesAdmin();
        return page == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(page);
    }

    @PutMapping("/api/licenses")
    @PreAuthorize("hasAuthority('FUNC_LICENSES_MANAGE')")
    public LicensesResponse updateLicenses(@RequestBody UpdateLicensesRequest request) {
        return appConfigService.updateLicenses(request);
    }
}
