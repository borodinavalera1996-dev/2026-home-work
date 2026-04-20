package company.vk.edu.distrib.compute.dummy;

import company.vk.edu.distrib.compute.KVCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DummyKVCluster implements KVCluster {
    private static final Logger log = LoggerFactory.getLogger(DummyKVCluster.class);

    @Override
    public void start() {
        log.info("cluster started");
    }

    @Override
    public void start(String endpoint) {
        log.info("node {} started", endpoint);
    }

    @Override
    public void stop() {
        log.info("cluster stopped");
    }

    @Override
    public void stop(String endpoint) {
        log.info("node {} stopped", endpoint);
    }

    @Override
    public List<String> getEndpoints() {
        return List.of();
    }
}
