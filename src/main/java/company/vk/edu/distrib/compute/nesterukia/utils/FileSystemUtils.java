package company.vk.edu.distrib.compute.nesterukia.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

public final class FileSystemUtils {
    private static final int KEY_MAX_LENGTH = 255;

    public static Path getFilePath(String key, Path storageDir) {
        String sanitizedKey = sanitizeKey(key);
        return storageDir.resolve(sanitizedKey + ".key");
    }

    public static String sanitizeKey(String key) {
        return key.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    public static void validateKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        if (key.length() > KEY_MAX_LENGTH) {
            throw new IllegalArgumentException("Key too long (max 255 characters)");
        }
    }

    public static byte[] readFileContent(Path file) throws IOException {
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new NoSuchElementException();
        }

        if (Files.size(file) == 0) {
            return new byte[]{};
        }

        return Files.readAllBytes(file);
    }

    private FileSystemUtils() {
        // cannot have instances
    }
}
