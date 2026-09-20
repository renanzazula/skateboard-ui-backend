package com.skateboard.uibackend.client.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TempFiles#createSecureTempFile} just needs to produce a real,
 * writable temp file with the requested prefix/suffix — the owner-only
 * permission it also applies (on filesystems that support POSIX permissions)
 * isn't independently observable from a plain {@link Path}, and this suite
 * runs on Windows where that branch is a no-op by design (see the class
 * javadoc).
 */
class TempFilesTest {

    @Test
    void createsAWritableFileWithThePrefixAndSuffix() throws IOException {
        Path tempFile = TempFiles.createSecureTempFile("my-prefix-", "-my-suffix.tmp");
        try {
            assertThat(tempFile).exists();
            assertThat(tempFile.getFileName().toString()).startsWith("my-prefix-").endsWith("-my-suffix.tmp");
            Files.writeString(tempFile, "content");
            assertThat(Files.readString(tempFile)).isEqualTo("content");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void createsAFileEvenWithoutASuffix() throws IOException {
        Path tempFile = TempFiles.createSecureTempFile("no-suffix-", null);
        try {
            assertThat(tempFile).exists();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
