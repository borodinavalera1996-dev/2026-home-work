package company.vk.edu.distrib.compute.nesterukia.file_system;

import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.KVServiceFactory;
import company.vk.edu.distrib.compute.nesterukia.KVServiceImpl;

import java.io.IOException;

public class NesterukiaFileSystemKVServiceFactory extends KVServiceFactory {
    private static final String PERSISTENT_STORAGE_PATH = "storage";

    @Override
    protected KVService doCreate(int port) throws IOException {
        return new KVServiceImpl(
                port,
                new NesterukiaFileSystemKVDao(PERSISTENT_STORAGE_PATH)
        );
    }
}
