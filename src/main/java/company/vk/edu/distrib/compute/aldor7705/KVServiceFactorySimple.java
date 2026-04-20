package company.vk.edu.distrib.compute.aldor7705;

import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.KVServiceFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class KVServiceFactorySimple extends KVServiceFactory {
    private static final String STORAGE_DIR = "storage";

    @Override
    protected KVService doCreate(int port) throws IOException {
        Path pathOfStorage = Path.of(STORAGE_DIR);

        if (!Files.exists(pathOfStorage)) {
            Files.createDirectory(pathOfStorage);
        }
        Path filePath = pathOfStorage.resolve("storage_" + port + ".txt");
        return new KVServiceSimple(port, new EntityDao(filePath));
    }
}
