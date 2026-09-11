package com.skateboard.uibackend.client.support;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;

/**
 * Shared helper for the multipart-upload clients (AppConfigClient,
 * CampaignClient, UserClient) that stage an uploaded file to a temp file
 * before forwarding it downstream. {@link Files#createTempFile(String,
 * String, FileAttribute[])} with no attributes leaves the resulting
 * permissions up to the platform/umask default, which on a shared,
 * world-writable temp directory (the flagged java:S5443 hotspot) can leave
 * the file readable — or, depending on the umask, writable — by any other
 * local user for the brief window before {@code transferTo}/{@code
 * deleteQuietly} run. This restricts the file to owner-only access on
 * filesystems that support POSIX permissions (every real deploy target:
 * Railway/Linux, local dev on macOS/Linux), and falls back to the plain,
 * unrestricted temp file on filesystems that don't (Windows has no POSIX
 * permission model) rather than failing the upload.
 */
public final class TempFiles {

    private static final FileAttribute<?>[] OWNER_ONLY_ATTRIBUTE = ownerOnlyAttribute();

    private TempFiles() {
    }

    public static Path createSecureTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix, OWNER_ONLY_ATTRIBUTE);
    }

    private static FileAttribute<?>[] ownerOnlyAttribute() {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return new FileAttribute<?>[0];
        }
        EnumSet<PosixFilePermission> ownerOnly = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        return new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(ownerOnly)};
    }
}
