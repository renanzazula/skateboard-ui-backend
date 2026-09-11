package com.skateboard.uibackend.exception;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * The BFF's own frontend-facing error shape — deliberately separate from
 * whatever error body a downstream service returned, so internal
 * implementation details never leak through this boundary.
 */
public record ErrorResponse(String code, String message, String correlationId, OffsetDateTime timestamp) {

    public static ErrorResponse of(String code, String message, String correlationId) {
        return new ErrorResponse(code, message, correlationId, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
