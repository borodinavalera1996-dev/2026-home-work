package company.vk.edu.distrib.compute.ip;

import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.KVServiceFactory;
import java.io.IOException;

public class PopovIgorKVServiceFactoryImpl extends KVServiceFactory {
    @Override
    protected KVService doCreate(int port) throws IOException {
        return new PopovIgorKVService(port);
    }
}
