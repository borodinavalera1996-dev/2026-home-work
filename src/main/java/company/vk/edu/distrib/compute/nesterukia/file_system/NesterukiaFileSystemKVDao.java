package company.vk.edu.distrib.compute.nesterukia.file_system;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.nesterukia.utils.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static company.vk.edu.distrib.compute.nesterukia.utils.FileSystemUtils.getFilePath;
import static company.vk.edu.distrib.compute.nesterukia.utils.FileSystemUtils.validateKey;

public class NesterukiaFileSystemKVDao implements Dao<byte[]> {

    private final Path storageDir;
    private final Map<String, ReentrantReadWriteLock> keyLocks;

    public NesterukiaFileSystemKVDao(String storagePath) throws IOException {
        this.storageDir = Paths.get(storagePath);
        this.keyLocks = new ConcurrentHashMap<>();

        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        if (!Files.isDirectory(storageDir)) {
            throw new IllegalArgumentException("Path exists but is not a directory: " + storagePath);
        }
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        validateKey(key);
        Path file = getFilePath(key, storageDir);

        ReentrantReadWriteLock lock = getLock(key);
        lock.readLock().lock();
        try {
            if (!Files.exists(file)) {
                keyLocks.remove(key);
                throw new NoSuchElementException();
            }
            return FileSystemUtils.readFileContent(file);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void upsert(String key, byte[] value) throws IllegalArgumentException, IOException {
        validateKey(key);
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        Path file = getFilePath(key, storageDir);
        ReentrantReadWriteLock lock = getLock(key);

        lock.writeLock().lock();
        try {
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tempFile, value);
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        validateKey(key);
        Path file = getFilePath(key, storageDir);
        ReentrantReadWriteLock lock = getLock(key);

        lock.writeLock().lock();
        try {
            Files.deleteIfExists(file);
            keyLocks.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        // No need to close
    }

    private ReentrantReadWriteLock getLock(String key) {
        return keyLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
    }
}
