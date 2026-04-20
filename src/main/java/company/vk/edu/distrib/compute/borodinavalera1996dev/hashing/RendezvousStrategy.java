package company.vk.edu.distrib.compute.borodinavalera1996dev.hashing;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import company.vk.edu.distrib.compute.borodinavalera1996dev.cluster.Node;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class RendezvousStrategy implements HashingStrategy {

    private final HashFunction hf = Hashing.murmur3_128();
    private final Collection<Node> nodes;

    public RendezvousStrategy(Collection<Node> nodes) {
        this.nodes = nodes;
    }

    @Override
    public Node getNode(String key) {
        Node winner = null;
        long maxWeight = Long.MIN_VALUE;

        for (Node node : nodes) {
            long weight = hash(key + node);

            if (weight > maxWeight) {
                maxWeight = weight;
                winner = node;
            }
        }
        return winner;
    }

    private long hash(String key) {
        HashCode hc = hf.hashString(key, StandardCharsets.UTF_8);
        return hc.asLong();
    }
}
