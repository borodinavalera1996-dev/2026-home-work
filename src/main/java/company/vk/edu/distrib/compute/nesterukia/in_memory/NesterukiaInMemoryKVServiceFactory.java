package company.vk.edu.distrib.compute.nesterukia.in_memory;

import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.KVServiceFactory;
import company.vk.edu.distrib.compute.nesterukia.KVServiceImpl;

import java.io.IOException;

public class NesterukiaInMemoryKVServiceFactory extends KVServiceFactory {
    @Override
    protected KVService doCreate(int port) throws IOException {
        return new KVServiceImpl(
                port,
                new NesterukiaInMemoryKVDao()
        );
    }
}
