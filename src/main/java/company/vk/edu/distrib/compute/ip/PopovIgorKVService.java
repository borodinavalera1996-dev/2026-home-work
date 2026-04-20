package company.vk.edu.distrib.compute.ip;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.KVService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PopovIgorKVService implements KVService {
    private static final Logger log = LoggerFactory.getLogger(PopovIgorKVService.class);
    
    private static final String ENTITY_PATH = "/v0/entity";
    private static final String STATUS_PATH = "/v0/status";
    private static final String ID_PARAM = "id";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String OCTET_STREAM = "application/octet-stream";
    
    private static final String METHOD_GET = "GET";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_DELETE = "DELETE";

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_NOT_FOUND = 404;
    private static final int STATUS_METHOD_NOT_ALLOWED = 405;
    
    private static final int SERVER_BACKLOG = 512;

    private final HttpServer server;
    private final int port;
    private final PopovIgorKVDaoPersistent dao;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public PopovIgorKVService(int port) {
        this.port = port;
        try {
            this.dao = new PopovIgorKVDaoPersistent("data_" + port);
            this.server = HttpServer.create(new InetSocketAddress("localhost", this.port), SERVER_BACKLOG);
            initServerContexts();
        } catch (IOException e) {
            log.error("Failed to create HTTP server on port {}", port, e);
            throw new IllegalStateException("Initial server setup failed", e);
        }
    }

    private void initServerContexts() {
        server.createContext(STATUS_PATH, this::handleStatus);
        server.createContext(ENTITY_PATH, this::handleEntity);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        try (exchange) {
            int status = METHOD_GET.equals(exchange.getRequestMethod()) ? STATUS_OK : STATUS_METHOD_NOT_ALLOWED;
            exchange.sendResponseHeaders(status, -1);
        }
    }

    private void handleEntity(HttpExchange exchange) {
        try (exchange) {
            if (!ENTITY_PATH.equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(STATUS_NOT_FOUND, -1);
                return;
            }

            String id = extractId(exchange.getRequestURI().getQuery());
            if (id == null || id.isEmpty()) {
                exchange.sendResponseHeaders(STATUS_BAD_REQUEST, -1);
                return;
            }

            String method = exchange.getRequestMethod();
            switch (method) {
                case METHOD_GET -> handleGet(exchange, id);
                case METHOD_PUT -> handlePut(exchange, id);
                case METHOD_DELETE -> handleDelete(exchange, id);
                default -> exchange.sendResponseHeaders(STATUS_METHOD_NOT_ALLOWED, -1);
            }
        } catch (Exception e) {
            log.error("Internal error during request handling", e);
            sendSafeResponse(exchange, STATUS_BAD_REQUEST);
        }
    }

    private String extractId(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String param : query.split("&")) {
            int eqIdx = param.indexOf('=');
            if (eqIdx != -1 && ID_PARAM.equals(param.substring(0, eqIdx))) {
                return param.substring(eqIdx + 1);
            }
        }
        return null;
    }

    private void handleGet(HttpExchange exchange, String id) throws IOException {
        try {
            byte[] response = dao.get(id);

            exchange.getResponseHeaders().set(CONTENT_TYPE_HEADER, OCTET_STREAM);
            exchange.sendResponseHeaders(STATUS_OK, response.length == 0 ? -1 : response.length);
            if (response.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            }
        } catch (NoSuchFileException e) {
            exchange.sendResponseHeaders(STATUS_NOT_FOUND, -1);
        }
    }

    private void handlePut(HttpExchange exchange, String id) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        dao.upsert(id, body);
        exchange.sendResponseHeaders(STATUS_CREATED, -1);
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        dao.delete(id);
        exchange.sendResponseHeaders(STATUS_ACCEPTED, -1);
    }

    private void sendSafeResponse(HttpExchange exchange, int code) {
        try {
            exchange.sendResponseHeaders(code, -1);
        } catch (IOException e) {
            log.error("Failed to send error response", e);
        }
    }

    @Override
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();
            log.info("Server started on port {}", port);
        }
    }

    @Override
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("Stopping server on port {}...", port);
            server.stop(0);
            log.info("Server stopped.");
            dao.close();
            log.info("DAO closed.");
        }
    }
}
