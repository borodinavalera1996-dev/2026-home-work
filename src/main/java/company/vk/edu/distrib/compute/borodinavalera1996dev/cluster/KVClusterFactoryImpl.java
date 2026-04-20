package company.vk.edu.distrib.compute.borodinavalera1996dev.cluster;

import company.vk.edu.distrib.compute.KVCluster;
import company.vk.edu.distrib.compute.KVClusterFactory;
import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.borodinavalera1996dev.InMemoryDao;
import company.vk.edu.distrib.compute.borodinavalera1996dev.KVProxyClient;
import company.vk.edu.distrib.compute.borodinavalera1996dev.KVServiceImpl;
import company.vk.edu.distrib.compute.borodinavalera1996dev.hashing.ConsistentStrategy;
import company.vk.edu.distrib.compute.borodinavalera1996dev.hashing.HashingStrategy;
import company.vk.edu.distrib.compute.borodinavalera1996dev.hashing.RendezvousStrategy;
import org.h2.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KVClusterFactoryImpl extends KVClusterFactory {
    @Override
    protected KVCluster doCreate(List<Integer> ports) {
        String endpointPrefix = "http://localhost:";
        List<Node> nodes = new ArrayList<>();
        for (Integer port : ports) {
            nodes.add(new Node(endpointPrefix + port));
        }
        HashingStrategy strategy = getHashingStrategy(nodes);

        KVProxyClient proxyClient = new KVProxyClient();
        Map<String, KVService> nodesMap = new ConcurrentHashMap<>();
        for (Integer port : ports) {
            String url = endpointPrefix + port;
            nodesMap.put(url, nodeService(port, url, strategy, proxyClient));
        }
        return new KVClusterImpl(nodesMap);
    }

    private static HashingStrategy getHashingStrategy(List<Node> nodes) {
        String hashingStrategy = System.getProperty("hashingStrategy");
        if (StringUtils.isNullOrEmpty(hashingStrategy) || "consistent".equals(hashingStrategy)) {
            return new ConsistentStrategy(nodes);
        } else {
            return new RendezvousStrategy(nodes);
        }
    }

    private static KVService nodeService(int port, String url, HashingStrategy strategy, KVProxyClient proxyClient) {
        try {
            return new KVServiceImpl(port, new InMemoryDao(), strategy, url, proxyClient);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
