package company.vk.edu.distrib.compute.borodinavalera1996dev.consensus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Consensus {
    private static final Logger LOG = LoggerFactory.getLogger(Consensus.class.getName());

    private static final Integer NODES_COUNT = 4;

    public static void main(String[] args) {
        try {
            List<Node> allNodes = new ArrayList<>();
            List<Thread> threads = new ArrayList<>();

            for (int i = 1; i <= NODES_COUNT; i++) {
                allNodes.add(new Node(i, allNodes));
            }

            LOG.info("Cluster initialization");
            for (Node node : allNodes) {
                Thread t = new Thread(node, "Node-" + node.getId());
                threads.add(t);
                t.start();
            }
            Random random = new Random();
            while (true) {
                TimeUnit.SECONDS.sleep(7);

                int nodeId = random.nextInt(NODES_COUNT);
                Node target = allNodes.get(nodeId);

                if (target.isEnabled()) {
                    LOG.info("Node {} is down", target.getId());
                    target.setEnabled(false);
                } else {
                    LOG.info("Node {} is up", target.getId());
                    target.setEnabled(true);
                    target.startElection();
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
