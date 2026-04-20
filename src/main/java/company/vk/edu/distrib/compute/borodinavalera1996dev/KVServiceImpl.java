package company.vk.edu.distrib.compute.borodinavalera1996dev;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.borodinavalera1996dev.cluster.Node;
import company.vk.edu.distrib.compute.borodinavalera1996dev.hashing.HashingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class KVServiceImpl implements KVService {
    private static final Logger log = LoggerFactory.getLogger(KVServiceImpl.class);
    public static final String GET = "GET";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String PATH_STATUS = "/v0/status";
    public static final String PATH_ENTITY = "/v0/entity";
    private final InetSocketAddress address;

    private HttpServer server;
    private final Dao<byte[]> dao;
    private HashingStrategy strategy;
    private KVProxyClient proxyClient;
    private String url;

    public KVServiceImpl(int port, Dao<byte[]> dao) throws IOException {
        this.dao = dao;
        this.address = new InetSocketAddress(port);
    }

    public KVServiceImpl(int port, Dao<byte[]> dao, HashingStrategy strategy,
                         String url, KVProxyClient proxyClient) throws IOException {
        this.dao = dao;
        this.address = new InetSocketAddress(port);
        this.strategy = strategy;
        this.proxyClient = proxyClient;
        this.url = url;
    }

    private void initServer() {
        server.createContext(PATH_STATUS, wrapHandler(exchange -> {
            String requestMethod = exchange.getRequestMethod();
            if (GET.equals(requestMethod)) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        }));
        server.createContext(PATH_ENTITY, wrapHandler(exchange -> {
            String requestMethod = exchange.getRequestMethod();
            String id = getId(exchange);
            if (id == null) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            if (callProxy(exchange, id)) {
                return;
            }
            switch (requestMethod) {
                case GET:
                    byte[] value = dao.get(id);
                    exchange.sendResponseHeaders(200, value.length);
                    exchange.getResponseBody().write(value);
                    break;
                case PUT:
                    dao.upsert(id, exchange.getRequestBody().readAllBytes());
                    exchange.sendResponseHeaders(201, -1);
                    break;
                case DELETE:
                    dao.delete(id);
                    exchange.sendResponseHeaders(202, -1);
                    break;
                default:
                    exchange.sendResponseHeaders(405, -1);
                    break;
            }
        }));
    }

    private boolean callProxy(HttpExchange exchange, String id) throws IOException {
        if (strategy != null) {
            Node responsibleNode = strategy.getNode(id);
            if (!responsibleNode.getName().equals(url)) {
                proxyClient.proxy(exchange, responsibleNode);
                return true;
            }
        }
        return false;
    }

    private HttpHandler wrapHandler(HttpHandler handler) {
        return exchange -> {
            try (exchange) {
                try {
                    handler.handle(exchange);
                } catch (NoSuchElementException e) {
                    sendError(exchange, 404, e.getMessage());
                } catch (IllegalArgumentException e) {
                    sendError(exchange, 400, e.getMessage());
                } catch (Exception e) {
                    sendError(exchange, 503, e.getMessage());
                }
            }
        };
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String response = message == null ? "" : message;
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static String getId(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        return Stream.of(query.split("&"))
                .filter(p -> p.startsWith("id="))
                .map(p -> p.split("="))
                .filter(parts -> parts.length > 1)
                .map(parts -> parts[1])
                .filter(val -> !val.isEmpty())
                .findFirst()
                .orElse(null);
    }

    @Override
    public void start() {
        try {
            server = HttpServer.create(address, 0);
            initServer();
            server.start();
            log.info("Started {}", address);
        } catch (IOException e) {
            log.error("Server is failed to start in {}", address, e);
            throw new UncheckedIOException("Server is failed to start", e);
        }
    }

    @Override
    public void stop() {
        log.info("Stopping {}", address);
        server.stop(0);
    }
}
