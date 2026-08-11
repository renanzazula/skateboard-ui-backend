package com.skateboard.uibackend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsDownstreamServiceExceptionToItsCarriedStatusAndCode() {
        DownstreamServiceException ex = new DownstreamServiceException(
                HttpStatus.NOT_FOUND, "PODCAST_NOT_FOUND", "Podcast post not found");

        ResponseEntity<ErrorResponse> response = handler.handleDownstreamServiceException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("PODCAST_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Podcast post not found");
    }

    @Test
    void mapsServiceUnavailableDownstreamFailureTo503() {
        DownstreamServiceException ex = new DownstreamServiceException(
                HttpStatus.SERVICE_UNAVAILABLE, "PODCAST_SERVICE_UNAVAILABLE", "Podcast service is currently unavailable");

        ResponseEntity<ErrorResponse> response = handler.handleDownstreamServiceException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code()).isEqualTo("PODCAST_SERVICE_UNAVAILABLE");
    }

    @Test
    void mapsAccessDeniedExceptionTo403WithoutLeakingItsMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("secret internal detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
        assertThat(response.getBody().message()).doesNotContain("secret internal detail");
    }

    @Test
    void mapsUnexpectedExceptionTo500WithoutLeakingItsMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("npe at line 42"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("npe at line 42");
    }
}
