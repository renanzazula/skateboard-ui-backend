package com.skateboard.uibackend.exception;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class ClientAbortsTest {

    /** Jetty's equivalent of ClientAbortException, matched by simple name. */
    private static class EofException extends IOException {
    }

    @Test
    void recognisesTomcatClientAbortException() {
        assertThat(ClientAborts.isClientAbort(new ClientAbortException("aborted"))).isTrue();
    }

    @Test
    void recognisesEofException() {
        assertThat(ClientAborts.isClientAbort(new EOFException())).isTrue();
    }

    @Test
    void recognisesClientAbortWrappedInHttpMessageNotReadableException() {
        // The production shape: PUT body read fails mid-conversion, so Spring
        // hands us the wrapper and the abort is two levels down.
        HttpMessageNotReadableException wrapped = new HttpMessageNotReadableException(
                "I/O error while reading input message",
                new ClientAbortException(new EOFException()),
                new MockHttpInputMessage(new byte[0]));

        assertThat(ClientAborts.isClientAbort(wrapped)).isTrue();
    }

    @Test
    void recognisesBrokenPipeByMessageRegardlessOfExceptionType() {
        assertThat(ClientAborts.isClientAbort(new IOException("Broken pipe"))).isTrue();
        assertThat(ClientAborts.isClientAbort(new IOException("Connection reset by peer"))).isTrue();
    }

    @Test
    void recognisesNonTomcatContainerAbortBySimpleName() {
        assertThat(ClientAborts.isClientAbort(new RuntimeException("wrapped", new EofException()))).isTrue();
    }

    @Test
    void doesNotTreatMalformedJsonAsAClientAbort() {
        HttpMessageNotReadableException malformed = new HttpMessageNotReadableException(
                "JSON parse error",
                new IOException("Unexpected character ('}')"),
                new MockHttpInputMessage(new byte[0]));

        assertThat(ClientAborts.isClientAbort(malformed)).isFalse();
    }

    @Test
    void doesNotTreatAnOrdinaryIoFailureAsAClientAbort() {
        assertThat(ClientAborts.isClientAbort(new SocketTimeoutException("Read timed out"))).isFalse();
        assertThat(ClientAborts.isClientAbort(new IOException((String) null))).isFalse();
        assertThat(ClientAborts.isClientAbort(new RuntimeException("npe at line 42"))).isFalse();
    }

    @Test
    void terminatesOnASelfReferencingCauseChain() {
        // A cause cycle would otherwise spin forever inside the error path.
        IOException cyclic = new IOException("boom") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        assertThat(ClientAborts.isClientAbort(cyclic)).isFalse();
    }
}
