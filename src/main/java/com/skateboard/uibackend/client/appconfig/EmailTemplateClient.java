package com.skateboard.uibackend.client.appconfig;

import com.skateboard.uibackend.client.appconfig.generated.api.EmailTemplatesApi;
import com.skateboard.uibackend.client.appconfig.generated.model.EmailTemplateResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateEmailTemplateRequest;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Wraps the generated {@link EmailTemplatesApi}, mirroring {@link
 * GuestApplicationSettingsClient}'s blocking-call and exception-mapping
 * shape. Kept separate from {@link AppConfigClient} for the same
 * per-feature reason {@link CampaignClient} is.
 */
@Component
public class EmailTemplateClient {

    private final EmailTemplatesApi emailTemplatesApi;

    public EmailTemplateClient(EmailTemplatesApi emailTemplatesApi) {
        this.emailTemplatesApi = emailTemplatesApi;
    }

    public List<EmailTemplateResponse> list() {
        return callList(emailTemplatesApi::listEmailTemplates);
    }

    // type is a plain String, not the generated enum — see
    // api/bff-openapi.yaml's getEmailTemplate parameter description.
    public EmailTemplateResponse get(String type, String language) {
        return call(() -> emailTemplatesApi.getEmailTemplate(type, language));
    }

    public EmailTemplateResponse update(UUID id, UpdateEmailTemplateRequest request) {
        return call(() -> emailTemplatesApi.updateEmailTemplate(id, request));
    }

    private <T> T call(Supplier<Mono<T>> invocation) {
        try {
            return invocation.get().block();
        } catch (WebClientResponseException ex) {
            throw mapResponseException(ex);
        } catch (WebClientRequestException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private <T> List<T> callList(Supplier<Flux<T>> invocation) {
        try {
            return invocation.get().collectList().block();
        } catch (WebClientResponseException ex) {
            throw mapResponseException(ex);
        } catch (WebClientRequestException ex) {
            throw serviceUnavailable(ex);
        }
    }

    private DownstreamServiceException mapResponseException(WebClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.is5xxServerError()) {
            return serviceUnavailable(ex);
        }
        return new DownstreamServiceException(status, codeFor(status), messageFor(status), ex);
    }

    private DownstreamServiceException serviceUnavailable(Throwable cause) {
        return new DownstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "APP_CONFIG_SERVICE_UNAVAILABLE",
                "App config service is currently unavailable", cause);
    }

    private static String codeFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "APP_CONFIG_NOT_FOUND";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "APP_CONFIG_BAD_REQUEST";
        }
        return "APP_CONFIG_REQUEST_ERROR";
    }

    private static String messageFor(HttpStatusCode status) {
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "Email template not found";
        }
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Subject and body are required, and may only use the variables this template type supports.";
        }
        return "App config service rejected the request";
    }
}
