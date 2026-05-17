package company.vk.edu.distrib.compute.borodinavalera1996dev.consensus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Consensus {
    private static final Logger LOG = LoggerFactory.getLogger(Consensus.class.getName());

    private static final Integer NODES_COUNT = 4;

    private Consensus() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void main(String[] args) {
        try (ExecutorService executor = Executors.newCachedThreadPool()) {
            List<Node> allNodes = new ArrayList<>();

            for (int i = 1; i <= NODES_COUNT; i++) {
                allNodes.add(new Node(i, allNodes));
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("Cluster initialization");
            }
            for (Node node : allNodes) {
                executor.execute(node);
            }
            run(allNodes);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void run(List<Node> allNodes) throws InterruptedException {
        Random random = new Random();
        while (true) {
            TimeUnit.SECONDS.sleep(7);

            int nodeId = random.nextInt(NODES_COUNT);
            Node target = allNodes.get(nodeId);

            if (target.isEnabled()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Node {} is down", target.getId());
                }
                target.setEnabled(false);
            } else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Node {} is up", target.getId());
                }
                target.setEnabled(true);
                target.startElection();
            }
        }
    }
}
