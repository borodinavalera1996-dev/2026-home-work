package company.vk.edu.distrib.compute.borodinavalera1996dev;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryDao implements Dao<byte[]> {

    private final ConcurrentMap<String, byte[]> storage;

    public InMemoryDao() {
        storage = new ConcurrentHashMap<>();
    }

    @Override
    public void close() throws IOException {
        //nope
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        validateKey(key);
        if (storage.get(key) == null) {
            throw new NoSuchElementException("Element not found for key - " + key);
        }
        return storage.get(key);
    }

    @Override
    public void upsert(String key, byte[] value) throws IllegalArgumentException, IOException {
        validateKey(key);
        storage.put(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        validateKey(key);
        storage.remove(key);
    }

    private static void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Id isn't be blank");
        }
    }
}
