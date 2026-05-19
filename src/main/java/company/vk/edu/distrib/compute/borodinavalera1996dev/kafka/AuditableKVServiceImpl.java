package company.vk.edu.distrib.compute.borodinavalera1996dev.kafka;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.AuditEvent;
import company.vk.edu.distrib.compute.AuditableKVService;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.borodinavalera1996dev.KVServiceImpl;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AuditableKVServiceImpl implements AuditableKVService {

    private static final Logger log = LoggerFactory.getLogger(AuditableKVServiceImpl.class);
    private static final String AUDIT_TOPIC = "audit";
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_DELETE = "DELETE";
    public static final String PATH_ENTITY = "/v0/entity";

    protected final int servicePort;
    private final AtomicBoolean async = new AtomicBoolean(true);
    private Producer<String, String> producer;
    private HttpServer server;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Dao<byte[]> dao;
    private final CompletableFuture<Void> termination = new CompletableFuture<>();

    public AuditableKVServiceImpl(int port, Dao<byte[]> dao) {
        servicePort = port;
        this.dao = dao;
    }

    @Override
    public void setBootstrapServers(String bootstrapServers) {
        Producer<String, String> oldProducer = producer;
        producer = new KafkaProducer<>(producerProperties(bootstrapServers));
        if (oldProducer != null) {
            oldProducer.close();
        }
    }

    @Override
    public void setAsync(boolean enabled) {
        async.set(enabled);
    }

    @Override
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(servicePort), 0);
            createKVContext();
            server.start();
            log.info("Started {}", servicePort);
        } catch (IOException e) {
            log.error("Server is failed to start in {}", servicePort, e);
            throw new IllegalStateException("Server is failed to start", e);
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (server != null) {
            server.stop(0);
        }
        if (producer != null) {
            try {
                producer.flush();
                producer.close(Duration.ofSeconds(3));
            } catch (Exception e) {
                log.error("Exception by closing kafka producer", e);
            }
        }
        termination.complete(null);
    }

    protected void createKVContext() {
        server.createContext(PATH_ENTITY, wrapHandler(getKVHttpHandler()));
    }

    protected HttpHandler getKVHttpHandler() {
        return exchange -> {
            String requestMethod = exchange.getRequestMethod();
            Map<String, String> parms = getParms(exchange);
            String id = parms.get("id");
            if (id == null || id.isBlank()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            audit(exchange.getRequestMethod(), id, System.currentTimeMillis());

            switch (requestMethod) {
                case HTTP_METHOD_GET:
                    byte[] value = dao.get(id);
                    exchange.sendResponseHeaders(200, value.length);
                    exchange.getResponseBody().write(value);
                    break;
                case HTTP_METHOD_PUT:
                    dao.upsert(id, exchange.getRequestBody().readAllBytes());
                    exchange.sendResponseHeaders(201, -1);
                    break;
                case HTTP_METHOD_DELETE:
                    dao.delete(id);
                    exchange.sendResponseHeaders(202, -1);
                    break;
                default:
                    exchange.sendResponseHeaders(405, -1);
                    break;
            }
        };
    }

    protected static Map<String, String> getParms(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();

        return (query == null || query.isEmpty())
                ? Collections.emptyMap()
                : Arrays.stream(query.split("&"))
                .map(param -> param.split("=", 2))
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> parts.length > 1 ? parts[1] : "",
                        (existing, replacement) -> existing
                ));
    }

    protected HttpHandler wrapHandler(HttpHandler handler) {
        return exchange -> {
            try (exchange) {
                try {
                    handler.handle(exchange);
                } catch (NoSuchElementException e) {
                    sendError(exchange, 404, e.getMessage());
                } catch (IllegalArgumentException e) {
                    sendError(exchange, 400, e.getMessage());
                } catch (KVServiceImpl.NotEnoughReplicasException e) {
                    sendError(exchange, 500, e.getMessage());
                } catch (Exception e) {
                    sendError(exchange, 503, e.getMessage());
                }
            }
        };
    }

    private void audit(String method, String id, long timestamp) {
        Producer<String, String> currentProducer = producer;
        if (currentProducer == null) {
            return;
        }
        ProducerRecord<String, String> record = new ProducerRecord<>(
                AUDIT_TOPIC,
                id,
                AuditEventCodec.serialize(new AuditEvent(method, id, timestamp))
        );
        if (async.get()) {
            currentProducer.send(record);
        } else {
            sendSync(currentProducer, record);
        }
    }

    private void sendSync(Producer<String, String> currentProducer, ProducerRecord<String, String> record) {
        try {
            currentProducer.send(record).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending audit event", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to send audit event", e);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String response = message == null ? "" : message;
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private Properties producerProperties(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.LINGER_MS_CONFIG, "10");
        return properties;
    }
}
