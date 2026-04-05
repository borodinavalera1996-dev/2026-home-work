package company.vk.edu.distrib.compute.borodinavalera1996dev;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    private final HttpServer server;
    private final Dao<byte[]> dao;

    public KVServiceImpl(int port, Dao<byte[]> dao) throws IOException {
        this.dao = dao;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        initServer();
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
        server.start();
        log.info("Started");
    }

    @Override
    public void stop() {
        log.info("Stopping");
        server.stop(0);
    }
}
