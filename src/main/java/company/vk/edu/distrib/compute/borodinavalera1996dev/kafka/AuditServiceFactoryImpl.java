package company.vk.edu.distrib.compute.borodinavalera1996dev.kafka;

import company.vk.edu.distrib.compute.AuditService;
import company.vk.edu.distrib.compute.AuditServiceFactory;

import java.io.IOException;

public class AuditServiceFactoryImpl extends AuditServiceFactory {
    @Override
    protected AuditService doCreate(String bootstrapServers, String consumerGroupId) throws IOException {
        return new AuditServiceImpl(bootstrapServers, consumerGroupId);
    }

}
