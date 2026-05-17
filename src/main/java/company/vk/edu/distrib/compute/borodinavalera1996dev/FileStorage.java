package company.vk.edu.distrib.compute.borodinavalera1996dev;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

public record FileStorage(Path path) {
    private static final ReentrantLock LOCK = new ReentrantLock();

    public record Data(byte[] value, Instant time, Boolean deleted) {
    }

    public FileStorage(Path path) {
        this.path = path;
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Can't create path", e);
        }
    }

    public void save(String key, Data data) {
        checkKey(key);
        Path filePath = path.resolve(key);
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1 + Long.BYTES + data.value.length);
            buffer.put((byte) 0);
            buffer.putLong(data.time.toEpochMilli());
            buffer.put(data.value);
            LOCK.lock();
            try {
                Files.write(filePath, buffer.array(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } finally {
                LOCK.unlock();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error save", e);
        }
    }

    public Data readFromFile(String key) {
        checkKey(key);
        Path filePath = path.resolve(key);

        if (!Files.exists(filePath)) {
            throw new NoSuchElementException("Element with key " + key + " not found");
        }
        byte[] allBytes = getBytes(filePath);
        validate(key, allBytes);

        ByteBuffer buffer = ByteBuffer.wrap(allBytes);
        boolean isDeleted = buffer.get() == 1;
        long millis = buffer.getLong();

        byte[] data = null;
        if (!isDeleted) {
            data = new byte[allBytes.length - (1 + Long.BYTES)];
            buffer.get(data);
        }

        return new Data(data, Instant.ofEpochMilli(millis), isDeleted);
    }

    private static void validate(String key, byte[] allBytes) {
        if (allBytes.length < 1 + Long.BYTES) {
            throw new IllegalStateException("File corrupted: too small " + key);
        }
    }

    private static byte[] getBytes(Path filePath) {
        byte[] allBytes;
        try {
            allBytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Error read file", e);
        }
        return allBytes;
    }

    public void deleteFromFile(String key) {
        Path filePath = path.resolve(key);
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1 + Long.BYTES);
            buffer.put((byte) 1);
            buffer.putLong(Instant.now().toEpochMilli());
            LOCK.lock();
            try {
                Files.write(filePath, buffer.array(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } finally {
                LOCK.unlock();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void checkKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key is null or empty");
        }
    }
}
