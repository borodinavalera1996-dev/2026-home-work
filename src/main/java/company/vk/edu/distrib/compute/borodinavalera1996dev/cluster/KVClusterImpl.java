package company.vk.edu.distrib.compute.borodinavalera1996dev.cluster;

import company.vk.edu.distrib.compute.KVCluster;
import company.vk.edu.distrib.compute.KVService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class KVClusterImpl implements KVCluster {

    private static final Logger log = LoggerFactory.getLogger(KVClusterImpl.class);

    private final Map<String, KVService> nodes;

    public KVClusterImpl(Map<String, KVService> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void start() {
        for (Map.Entry<String, KVService> entry : nodes.entrySet()) {
            entry.getValue().start();
        }
        log.info("cluster started");
    }

    @Override
    public void start(String endpoint) {
        KVService service = nodes.get(endpoint);
        if (service == null) {
            throw new IllegalArgumentException(String.format("Endpoint {} does not exist.", endpoint));
        }
        service.start();
        log.info("node {} started", endpoint);
    }

    @Override
    public void stop() {
        for (Map.Entry<String, KVService> entry : nodes.entrySet()) {
            entry.getValue().stop();
        }
        log.info("cluster stopped");
    }

    @Override
    public void stop(String endpoint) {
        KVService service = nodes.get(endpoint);
        if (service == null) {
            throw new IllegalArgumentException(String.format("Endpoint {} does not exist.", endpoint));
        }
        service.stop();
        log.info("node {} stopped", endpoint);
    }

    @Override
    public List<String> getEndpoints() {
        return List.copyOf(nodes.keySet());
    }
}
