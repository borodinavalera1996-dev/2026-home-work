package company.vk.edu.distrib.compute.dummy;

import company.vk.edu.distrib.compute.KVCluster;
import company.vk.edu.distrib.compute.KVClusterFactory;

import java.util.List;

public class DummyKVClusterFactory extends KVClusterFactory {
    @Override
    protected KVCluster doCreate(List<Integer> ports) {
        return new DummyKVCluster();
    }
}
