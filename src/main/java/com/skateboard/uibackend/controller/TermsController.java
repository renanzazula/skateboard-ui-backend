package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.TermsResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateTermsRequest;
import com.skateboard.uibackend.service.AppConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Passes the frontend straight through to skateboard-app-config-be's
 * {@code /api/terms} routes, mirroring {@link AboutUsController}.
 * <p>
 * {@code GET /api/terms} backs the standard-user viewer, so it is gated by
 * {@code FUNC_TAB_SETTINGS} (the baseline authority every user holds) rather
 * than the admin-only {@code FUNC_TERMS_MANAGE} the other two require. A
 * {@code null} from the service means app-config-be answered {@code 204} (no
 * page published/created yet) — relayed as {@code 204} here too.
 */
@RestController
public class TermsController {

    private final AppConfigService appConfigService;

    public TermsController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping("/api/terms")
    @PreAuthorize("hasAuthority('FUNC_TAB_SETTINGS')")
    public ResponseEntity<TermsResponse> getTerms() {
        TermsResponse page = appConfigService.getTerms();
        return page == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(page);
    }

    @GetMapping("/api/terms/admin")
    @PreAuthorize("hasAuthority('FUNC_TERMS_MANAGE')")
    public ResponseEntity<TermsResponse> getTermsAdmin() {
        TermsResponse page = appConfigService.getTermsAdmin();
        return page == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(page);
    }

    @PutMapping("/api/terms")
    @PreAuthorize("hasAuthority('FUNC_TERMS_MANAGE')")
    public TermsResponse updateTerms(@RequestBody UpdateTermsRequest request) {
        return appConfigService.updateTerms(request);
    }
}
