package company.vk.edu.distrib.compute.ip;

import company.vk.edu.distrib.compute.Dao;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public class PopovIgorKVDaoPersistent implements Dao<byte[]> {
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Path storageDir;

    public PopovIgorKVDaoPersistent(String dataPath) throws IOException {
        storageDir = Paths.get(dataPath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
    }

    @Override
    public byte[] get(String key) throws IOException {
        checkActive();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        final Path file = storageDir.resolve(Paths.get(key).getFileName().toString());
        return Files.readAllBytes(file);
    }

    @Override
    public void upsert(String key, byte[] value) throws IOException {
        checkActive();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        final Path file = storageDir.resolve(Paths.get(key).getFileName().toString());
        Files.write(file, value);
    }

    @Override
    public void delete(String key) throws IOException {
        checkActive();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        final Path file = storageDir.resolve(Paths.get(key).getFileName().toString());
        Files.deleteIfExists(file);
    }

    @Override
    public void close() {
        active.set(false);
    }

    private void checkActive() throws IOException {
        if (!active.get()) {
            throw new IOException("DAO is already closed");
        }
    }
}
