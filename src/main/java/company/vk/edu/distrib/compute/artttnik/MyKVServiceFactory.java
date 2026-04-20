package company.vk.edu.distrib.compute.artttnik;

import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.KVServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

public class MyKVServiceFactory extends KVServiceFactory {
    private static final Logger log = LoggerFactory.getLogger(MyKVServiceFactory.class);

    @Override
    protected KVService doCreate(int port) {
        log.debug("Creating KVService on port {}", port);
        return new MyKVService(port, new MyDao(Path.of("data")));
    }
}
