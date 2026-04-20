package company.vk.edu.distrib.compute.ip;

import company.vk.edu.distrib.compute.Dao;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PopovIgorKVDao implements Dao<byte[]> {
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public byte[] get(String key) throws IOException {
        checkActive();
        return storage.get(key);
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
        storage.put(key, value);
    }

    @Override
    public void delete(String key) throws IOException {
        checkActive();
        storage.remove(key);
    }

    @Override
    public void close() {
        if (active.compareAndSet(true, false)) {
            storage.clear();
        }
    }

    private void checkActive() throws IOException {
        if (!active.get()) {
            throw new IOException("DAO is already closed");
        }
    }
}
