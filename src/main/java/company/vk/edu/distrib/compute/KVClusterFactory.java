package company.vk.edu.distrib.compute;

import java.util.List;

public abstract class KVClusterFactory {
    public KVCluster create(List<Integer> ports) {
        if (ports == null || ports.isEmpty()) {
            throw new IllegalArgumentException("Missing ports for the cluster");
        }

        return doCreate(ports);
    }

    protected abstract KVCluster doCreate(List<Integer> ports);
}
