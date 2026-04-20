package company.vk.edu.distrib.compute.aldor7705;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.aldor7705.storage.DaoFileStorage;

import java.nio.file.Path;

public class EntityDao implements Dao<byte[]> {

    public final DaoFileStorage storage;

    public EntityDao(Path path) {
        this.storage = new DaoFileStorage(path);
    }

    @Override
    public byte[] get(String key) {
        return storage.readFromFile(key);
    }

    @Override
    public void upsert(String key, byte[] value) {
        storage.save(key, value);
    }

    @Override
    public void delete(String key) {
        storage.deleteFromFile(key);
    }

    @Override
    public void close() {
        storage.dropStorage();
    }
}
