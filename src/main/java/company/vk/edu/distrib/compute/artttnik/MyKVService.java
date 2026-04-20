package company.vk.edu.distrib.compute.artttnik;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.artttnik.exception.ServerStartException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyKVService implements KVService {
    private static final Logger log = LoggerFactory.getLogger(MyKVService.class);

    private final int port;
    private ExecutorService executor;
    private final Dao<byte[]> dao;
    private HttpServer server;

    public MyKVService(int port, Dao<byte[]> dao) {
        this.port = port;
        this.dao = dao;
    }

    @Override
    public void start() {
        try {
            server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), port),
                    0
            );

            server.createContext("/v0/status", new StatusHandler());
            server.createContext("/v0/entity", new EntityHandler(dao));

            executor = Executors.newFixedThreadPool(4);
            server.setExecutor(executor);

            server.start();
            log.info("KVService started on port {}", port);
        } catch (IOException e) {
            log.error("Failed to start server on port {}", port, e);
            throw new ServerStartException("Failed to start server on port " + port, e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }

        if (executor != null) {
            executor.shutdown();
        }

        try {
            dao.close();
        } catch (IOException e) {
            log.debug("Failed to close Dao", e);
        }

        log.info("KVService stopped");
    }

    private static final class StatusHandler implements HttpHandler {
        private static final Logger log = LoggerFactory.getLogger(StatusHandler.class);

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                if (log.isErrorEnabled()) {
                    log.error("Unsupported method for /v0/status: {}", exchange.getRequestMethod());
                }
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private record EntityHandler(Dao<byte[]> dao) implements HttpHandler {
        private static final String ID_PARAM = "id";
        private static final Logger log = LoggerFactory.getLogger(EntityHandler.class);

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            var id = extractId(exchange.getRequestURI().getRawQuery());

            if (id == null || id.isEmpty()) {
                log.warn("Bad request: empty id value");
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            try {
                dispatch(exchange, id);
            } catch (NoSuchElementException e) {
                log.error("Entity not found: {}", id);
                exchange.sendResponseHeaders(404, -1);
            } catch (IllegalArgumentException e) {
                if (log.isErrorEnabled()) {
                    log.error("Invalid argument: {}", e.getMessage());
                }
                exchange.sendResponseHeaders(400, -1);
            } catch (Exception e) {
                log.error("Internal error processing request", e);
                exchange.sendResponseHeaders(500, -1);
            }
        }

        private void dispatch(HttpExchange exchange, String id) throws IOException {
            var method = exchange.getRequestMethod();
            switch (method) {
                case "GET" -> handleGet(exchange, id);
                case "PUT" -> handlePut(exchange, id);
                case "DELETE" -> handleDelete(exchange, id);
                default -> {
                    log.warn("Unsupported method: {}", method);
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        }

        private void handleGet(HttpExchange exchange, String id) throws IOException {
            var value = dao.get(id);

            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, value.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(value);
            }
        }

        private void handlePut(HttpExchange exchange, String id) throws IOException {
            try (InputStream is = exchange.getRequestBody()) {
                var body = is.readAllBytes();

                dao.upsert(id, body);
            }

            exchange.sendResponseHeaders(201, -1);
        }

        private void handleDelete(HttpExchange exchange, String id) throws IOException {
            dao.delete(id);

            exchange.sendResponseHeaders(202, -1);
        }

        private static String extractId(String query) {
            if (query == null || query.isEmpty()) {
                return null;
            }

            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);

                if (kv.length == 2 && ID_PARAM.equals(kv[0])) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }

            return null;
        }
    }
}
