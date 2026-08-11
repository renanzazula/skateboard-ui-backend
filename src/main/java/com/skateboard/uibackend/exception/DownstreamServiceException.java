package com.skateboard.uibackend.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Wraps a failure talking to a downstream service, carrying the HTTP status
 * and machine-readable code {@link com.skateboard.uibackend.exception.GlobalExceptionHandler}
 * should respond to the frontend with, instead of leaking the downstream
 * response/stack trace directly.
 */
public class DownstreamServiceException extends RuntimeException {

    private final HttpStatusCode status;
    private final String code;

    public DownstreamServiceException(HttpStatusCode status, String code, String message) {
        this(status, code, message, null);
    }

    public DownstreamServiceException(HttpStatusCode status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
