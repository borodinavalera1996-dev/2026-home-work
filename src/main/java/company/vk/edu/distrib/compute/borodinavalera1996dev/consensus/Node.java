package company.vk.edu.distrib.compute.borodinavalera1996dev.consensus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Node implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Node.class.getName());
    private static final long TIMEOUT_MS = 3000;

    private final int id;
    private final List<Node> allNodes;
    private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

    private final AtomicInteger currentLeaderId = new AtomicInteger(-1);
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean waitingForAnswer = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong lastLeaderResponseTime = new AtomicLong(System.currentTimeMillis());

    public Node(int id, List<Node> allNodes) {
        this.id = id;
        this.allNodes = allNodes;
    }

    @Override
    public void run() {
        startHealthCheck();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Message msg = queue.take();
                if (isRunning.get()) {
                    handleMessage(msg);
                }
            } catch (InterruptedException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getMessage());
                }
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.type()) {
            case ELECT -> {
                if (msg.idNode() < this.id) {
                    send(msg.idNode(), Message.Type.ANSWER);
                    startElection();
                }
            }
            case VICTORY -> {
                this.currentLeaderId.set(msg.idNode());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Node {}: New leader is {}", id, currentLeaderId);
                }
            }
            case PING ->
                send(msg.idNode(), Message.Type.ANSWER);
            case ANSWER ->
                waitingForAnswer.set(true);
        }
    }

    private void send(int targetId, Message.Type type) {
        allNodes.stream()
                .filter(node -> node.id == targetId)
                .findFirst()
                .ifPresent(node -> node.receive(new Message(this.id, type)));
    }

    public void startElection() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Node {} starts election", id);
        }
        List<Node> higherNodes = allNodes.stream()
                .filter(n -> n.id > this.id)
                .toList();

        if (higherNodes.isEmpty()) {
            becomeLeader();
            return;
        }

        waitingForAnswer.set(false);
        higherNodes.forEach(n -> n.receive(new Message(this.id, Message.Type.ELECT)));

        scheduler.schedule(() -> {
            if (!waitingForAnswer.get()) {
                becomeLeader();
            }
        }, 1, TimeUnit.SECONDS);
    }

    private void becomeLeader() {
        this.currentLeaderId.set(id);
        allNodes.forEach(n -> n.receive(new Message(id, Message.Type.VICTORY)));
    }

    private void startHealthCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning.get()) {
                return;
            }
            if (currentLeaderId.get() == -1) {
                startElection();
                return;
            }
            if (currentLeaderId.get() == id) {
                return;
            }

            long now = System.currentTimeMillis();

            if (now - lastLeaderResponseTime.get() > TIMEOUT_MS) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Node {}: Leader {} timeout! Starting election...", id, currentLeaderId);
                }
                currentLeaderId.set(-1);
                startElection();
            } else {
                send(currentLeaderId.get(), Message.Type.PING);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void receive(Message msg) {
        queue.offer(msg);
    }

    public void setEnabled(boolean active) {
        isRunning.set(active);
    }

    public int getId() {
        return id;
    }

    public boolean isEnabled() {
        return isRunning.get();
    }
}
