package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.appconfig.generated.model.EmailTemplateResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateEmailTemplateRequest;
import com.skateboard.uibackend.service.EmailTemplateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Passes the frontend straight through to skateboard-app-config-be's
 * {@code /api/email-templates/**}, mirroring {@link AboutUsController}.
 * Every route here requires {@code FUNC_EMAIL_TEMPLATE_MANAGE}, copied
 * verbatim from the vendored spec's {@code x-required-permissions} — unlike
 * {@link GuestApplicationController}, there is no unauthenticated/public
 * route in this feature.
 */
@RestController
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    public EmailTemplateController(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    @GetMapping("/api/email-templates")
    @PreAuthorize("hasAuthority('FUNC_EMAIL_TEMPLATE_MANAGE')")
    public List<EmailTemplateResponse> list() {
        return emailTemplateService.list();
    }

    // type is a plain String path variable, not the generated enum — Spring's
    // default enum conversion (Enum.valueOf) doesn't survive openapi-generator
    // stripping this enum's shared "GUEST_APPLICATION_" prefix from its Java
    // constant names (see api/bff-openapi.yaml's parameter description).
    @GetMapping("/api/email-templates/{type}/{language}")
    @PreAuthorize("hasAuthority('FUNC_EMAIL_TEMPLATE_MANAGE')")
    public EmailTemplateResponse get(@PathVariable String type, @PathVariable String language) {
        return emailTemplateService.get(type, language);
    }

    @PutMapping("/api/email-templates/{id}")
    @PreAuthorize("hasAuthority('FUNC_EMAIL_TEMPLATE_MANAGE')")
    public EmailTemplateResponse update(@PathVariable UUID id, @RequestBody UpdateEmailTemplateRequest request) {
        return emailTemplateService.update(id, request);
    }
}
