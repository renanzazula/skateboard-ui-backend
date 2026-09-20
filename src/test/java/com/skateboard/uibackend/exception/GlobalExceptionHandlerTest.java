package com.skateboard.uibackend.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final Level originalLevel = logger.getLevel();

    @AfterEach
    void restoreLogLevel() {
        logger.setLevel(originalLevel);
    }

    @Test
    void debugLoggingWalksToTheRootCauseOfANestedClientAbort() {
        logger.setLevel(Level.DEBUG);

        ResponseEntity<ErrorResponse> response = handler.handleIoException(
                new ClientAbortException(new RuntimeException("outer", new EOFException("innermost"))));

        assertThat(response.getStatusCode().value()).isEqualTo(499);
        assertThat(response.getBody()).isNull();
    }

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

    @Test
    void mapsMalformedRequestBodyTo400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error",
                new IOException("Unexpected character ('}')"),
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST_BODY");
    }

    @Test
    void mapsAClientAbortWrappedInAnUnreadableBodyTo499WithNoBody() {
        // The reported production shape: a PUT whose caller disconnected
        // mid-upload used to fall through to the generic 500 branch and log a
        // full stack trace per disconnect.
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "I/O error while reading input message",
                new ClientAbortException(new EOFException()),
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(499);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void mapsAnUnwrappedClientAbortTo499WithNoBody() {
        ResponseEntity<ErrorResponse> response = handler.handleIoException(new ClientAbortException("aborted"));

        assertThat(response.getStatusCode().value()).isEqualTo(499);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void mapsAnUnwrappedEofExceptionTo499WithNoBody() {
        ResponseEntity<ErrorResponse> response = handler.handleIoException(new EOFException());

        assertThat(response.getStatusCode().value()).isEqualTo(499);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void stillMapsAGenuineIoFailureTo500() {
        ResponseEntity<ErrorResponse> response = handler.handleIoException(new SocketTimeoutException("Read timed out"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void catchAllHandlerAlsoRecognisesAClientAbortWrappedInAnUnexpectedType() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("write failed", new ClientAbortException("aborted")));

        assertThat(response.getStatusCode().value()).isEqualTo(499);
        assertThat(response.getBody()).isNull();
    }
}
