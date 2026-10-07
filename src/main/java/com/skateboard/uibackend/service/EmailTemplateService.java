package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.EmailTemplateClient;
import com.skateboard.uibackend.client.appconfig.generated.model.EmailTemplateResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateEmailTemplateRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Thin pass-through, same seam as {@link GuestApplicationService}/{@link AppConfigService}. */
@Service
public class EmailTemplateService {

    private final EmailTemplateClient emailTemplateClient;

    public EmailTemplateService(EmailTemplateClient emailTemplateClient) {
        this.emailTemplateClient = emailTemplateClient;
    }

    public List<EmailTemplateResponse> list() {
        return emailTemplateClient.list();
    }

    public EmailTemplateResponse get(String type, String language) {
        return emailTemplateClient.get(type, language);
    }

    public EmailTemplateResponse update(UUID id, UpdateEmailTemplateRequest request) {
        return emailTemplateClient.update(id, request);
    }
}
