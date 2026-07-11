package com.kaiki.minechatcorrect.config;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Archive path validation helpers shared by all dictionary archive import paths.
 */
public final class ArchiveEntrySafety {
    private ArchiveEntrySafety() {
    }

    public static Path safeResolve(Path root, String name) throws IOException {
        if (name == null) {
            throw new IOException("Unsafe archive entry: " + name);
        }

        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (name.isBlank()) {
            return normalizedRoot;
        }

        Path entryPath = Path.of(name);
        if (entryPath.isAbsolute()) {
            throw new IOException("Unsafe archive entry: " + name);
        }

        Path target = normalizedRoot.resolve(entryPath).normalize();
        if (!target.startsWith(normalizedRoot)) {
            throw new IOException("Unsafe archive entry: " + name);
        }

        return target;
    }
}
