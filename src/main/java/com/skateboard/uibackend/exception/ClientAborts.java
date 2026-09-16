package com.skateboard.uibackend.exception;

import java.io.EOFException;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Recognises "the client hung up mid-request" failures.
 *
 * <p>A mobile client that backgrounds the app, loses signal or navigates away
 * mid-PUT closes the socket while Tomcat is still reading the request body.
 * Tomcat surfaces that as {@code ClientAbortException} (an {@link IOException}),
 * and because the read happens inside Jackson's body conversion, Spring wraps
 * it in an {@code HttpMessageNotReadableException} before it ever reaches
 * {@link GlobalExceptionHandler}. Matching on the outermost type alone
 * therefore misses it, which is how these ended up in the generic 500 branch
 * logging a full stack trace per disconnect — enough volume on a flaky mobile
 * network to get log lines dropped by the platform.
 *
 * <p>Detection walks the whole cause chain and is deliberately server-agnostic
 * (matching on simple class name rather than importing
 * {@code org.apache.catalina.connector.ClientAbortException}), so swapping the
 * embedded container doesn't silently bring the noise back.
 */
final class ClientAborts {

    /**
     * Nginx's convention for "client closed the connection before we
     * answered". Never actually reaches the client — the socket is gone — but
     * it keeps the access log honest instead of recording a 200 or a 500.
     */
    static final int CLIENT_CLOSED_REQUEST = 499;

    private static final Set<String> ABORT_TYPES = Set.of(
            "ClientAbortException",            // Tomcat
            "EofException",                    // Jetty
            "AsyncRequestNotUsableException"   // Spring MVC: response no longer writable
    );

    private static final List<String> ABORT_MESSAGES = List.of(
            "broken pipe",
            "connection reset",
            "connection was aborted",
            "an existing connection was forcibly closed"
    );

    private ClientAborts() {
    }

    /**
     * @return true when {@code throwable}, or anything it wraps, is a client
     * disconnect rather than a fault on our side.
     */
    static boolean isClientAbort(Throwable throwable) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = throwable; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof EOFException) {
                return true;
            }
            if (ABORT_TYPES.contains(cause.getClass().getSimpleName())) {
                return true;
            }
            if (cause instanceof IOException && messageIndicatesAbort(cause.getMessage())) {
                return true;
            }
        }
        return false;
    }

    private static boolean messageIndicatesAbort(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return ABORT_MESSAGES.stream().anyMatch(normalized::contains);
    }
}
