package company.vk.edu.distrib.compute.borodinavalera1996dev.kafka;

import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.KVServiceFactory;
import company.vk.edu.distrib.compute.borodinavalera1996dev.InMemoryDao;

import java.io.IOException;

public class AuditableKVServiceFactory extends KVServiceFactory {

    @Override
    protected KVService doCreate(int port) throws IOException {
        return new AuditableKVServiceImpl(port, new InMemoryDao());
    }
}
