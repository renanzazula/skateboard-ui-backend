package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.PrivacyPolicyResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdatePrivacyPolicyRequest;
import com.skateboard.uibackend.service.AppConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Passes the frontend straight through to skateboard-app-config-be's
 * {@code /api/privacy-policy} routes, mirroring {@link AboutUsController}.
 * <p>
 * {@code GET /api/privacy-policy} has no {@code @PreAuthorize} at all —
 * unlike About Us's GET (gated by {@code FUNC_TAB_SETTINGS}), this route is
 * fully anonymous: {@code SecurityConfig} grants it {@code permitAll()}
 * because the App Store / Play Store listing links to it directly, and app
 * reviewers and signed-out users must be able to read it. A {@code null}
 * from the service means app-config-be answered {@code 204} (no page
 * published/created yet) — relayed as {@code 204} here too.
 */
@RestController
public class PrivacyPolicyController {

    private final AppConfigService appConfigService;

    public PrivacyPolicyController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping("/api/privacy-policy")
    public ResponseEntity<PrivacyPolicyResponse> getPrivacyPolicy() {
        PrivacyPolicyResponse page = appConfigService.getPrivacyPolicy();
        return page == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(page);
    }

    @GetMapping("/api/privacy-policy/admin")
    @PreAuthorize("hasAuthority('FUNC_PRIVACY_POLICY_MANAGE')")
    public ResponseEntity<PrivacyPolicyResponse> getPrivacyPolicyAdmin() {
        PrivacyPolicyResponse page = appConfigService.getPrivacyPolicyAdmin();
        return page == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(page);
    }

    @PutMapping("/api/privacy-policy")
    @PreAuthorize("hasAuthority('FUNC_PRIVACY_POLICY_MANAGE')")
    public PrivacyPolicyResponse updatePrivacyPolicy(@RequestBody UpdatePrivacyPolicyRequest request) {
        return appConfigService.updatePrivacyPolicy(request);
    }
}
