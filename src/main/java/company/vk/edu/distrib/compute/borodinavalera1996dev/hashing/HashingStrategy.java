package company.vk.edu.distrib.compute.borodinavalera1996dev.hashing;

import company.vk.edu.distrib.compute.borodinavalera1996dev.cluster.Node;

@FunctionalInterface
public interface HashingStrategy {
    Node getNode(String key);
}
