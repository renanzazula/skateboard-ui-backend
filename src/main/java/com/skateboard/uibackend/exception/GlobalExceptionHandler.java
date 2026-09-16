package com.skateboard.uibackend.exception;

import com.skateboard.uibackend.web.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

/**
 * Translates every exception a controller can throw into the same
 * {@link ErrorResponse} shape, so the frontend never sees a downstream
 * service's raw error body or a Java stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamServiceException(DownstreamServiceException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Downstream call failed: {}", ex.getMessage(), ex);
        } else {
            log.warn("Downstream call rejected: {}", ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), correlationId()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("FORBIDDEN", "You do not have permission to perform this action", correlationId()));
    }

    /**
     * An unreadable request body is either malformed JSON (the caller's bug,
     * 400) or the caller disconnecting mid-upload — Tomcat's
     * {@code ClientAbortException} reaches us wrapped in this type, since the
     * socket read happens inside Jackson's conversion. Only the first is worth
     * a 400 and a log line; see {@link #handleClientAbort}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        if (ClientAborts.isClientAbort(ex)) {
            return handleClientAbort(ex);
        }
        log.warn("Rejected malformed request body: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_REQUEST_BODY", "The request body could not be read", correlationId()));
    }

    /**
     * Covers {@code ClientAbortException} and {@link java.io.EOFException}
     * arriving unwrapped — a disconnect during a raw stream read rather than
     * during body conversion.
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIoException(IOException ex) {
        if (ClientAborts.isClientAbort(ex)) {
            return handleClientAbort(ex);
        }
        return handleUnexpected(ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // The abort can also surface wrapped in a type we don't name above
        // (a filter, an interceptor, a converter of its own). Checking here
        // too means no disconnect can reach the ERROR branch by a path we
        // didn't anticipate.
        if (ClientAborts.isClientAbort(ex)) {
            return handleClientAbort(ex);
        }
        log.error("Unexpected error handling request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred", correlationId()));
    }

    /**
     * The client is already gone, so there is nobody to send an error body to
     * and nothing on our side to fix: log one DEBUG line (no stack trace, so a
     * flaky mobile network can't flood the platform's log budget) and answer
     * 499 with no body.
     */
    private ResponseEntity<ErrorResponse> handleClientAbort(Throwable ex) {
        if (log.isDebugEnabled()) {
            log.debug("Client disconnected before the request completed: {}", rootMessage(ex));
        }
        return ResponseEntity.status(ClientAborts.CLIENT_CLOSED_REQUEST).build();
    }

    private String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private String correlationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
