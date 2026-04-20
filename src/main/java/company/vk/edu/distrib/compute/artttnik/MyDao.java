package company.vk.edu.distrib.compute.artttnik;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.artttnik.exception.StorageInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.NoSuchElementException;

public class MyDao implements Dao<byte[]> {
    private static final Logger log = LoggerFactory.getLogger(MyDao.class);
    private final Path storageDir;

    public MyDao(Path storageDir) {
        try {
            this.storageDir = storageDir;
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new StorageInitException("Failed to create storage directory", e);
        }
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        requireValidKey(key);

        var filePath = storageDir.resolve(sanitizeFileName(key));
        try {
            return Files.readAllBytes(filePath);
        } catch (NoSuchFileException e) {
            throw new NoSuchElementException("key not found: " + key, e);
        }
    }

    @Override
    public void upsert(String key, byte[] value) throws IllegalArgumentException, IOException {
        requireValidKey(key);

        var filePath = storageDir.resolve(sanitizeFileName(key));
        Files.write(filePath, value, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        requireValidKey(key);

        var filePath = storageDir.resolve(sanitizeFileName(key));
        Files.deleteIfExists(filePath);
    }

    @Override
    public void close() {
        log.debug("Closing Dao");
    }

    private void requireValidKey(String key) {
        if (key == null || key.isEmpty()) {
            log.warn("Attempt to access storage with null or empty key");
            throw new IllegalArgumentException("key is null or empty");
        }
    }

    private static String sanitizeFileName(String key) {
        return Base64.getUrlEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }
}
