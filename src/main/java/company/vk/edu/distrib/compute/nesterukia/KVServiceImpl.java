package company.vk.edu.distrib.compute.nesterukia;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.nesterukia.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import static company.vk.edu.distrib.compute.nesterukia.utils.HttpUtils.parseQueryParams;
import static company.vk.edu.distrib.compute.nesterukia.utils.HttpUtils.sendResponse;

public class KVServiceImpl implements KVService {
    private static final Logger log = LoggerFactory.getLogger(KVServiceImpl.class);

    private static final String BASE_URI = "/v0";
    private static final String STATUS_URI = "/status";
    private static final String ENTITY_URI = "/entity";
    private static final String ID_QUERY_PARAM = "id";
    private static final int SERVER_STOP_TIMEOUT_SEC = 1;

    private final HttpServer server;
    private final Dao<byte[]> dao;

    public KVServiceImpl(int port, Dao<byte[]> dao) throws IOException {
        this.server = HttpServer.create(
                new InetSocketAddress(port),
                0
        );
        this.dao = dao;
        initService();
        log.info("Server was initialized on port: {}", port);
    }

    private void initService() {
        server.createContext(BASE_URI.concat(STATUS_URI), new ErrorHttpHandler(http -> {
            final String requestMethod = http.getRequestMethod();
            if (Objects.equals(HttpUtils.MethodConstants.GET, requestMethod)) {
                sendResponse(http, HttpUtils.StatusConstants.OK);
            } else {
                sendResponse(http, HttpUtils.StatusConstants.METHOD_NOT_ALLOWED);
            }
            http.close();
        }));

        server.createContext(BASE_URI.concat(ENTITY_URI), new ErrorHttpHandler(http -> {
            final String requestMethod = http.getRequestMethod();
            final Map<String, String> queryParams = parseQueryParams(http);

            final String entityId = queryParams.get(ID_QUERY_PARAM);
            if (entityId == null) {
                log.error("Query parameter 'id' is mandatory and must be set.");
                throw new IllegalArgumentException();
            }

            switch (requestMethod) {
                case HttpUtils.MethodConstants.GET -> {
                    byte[] entityValue = dao.get(entityId);
                    sendResponse(http, HttpUtils.StatusConstants.OK, entityValue);
                }
                case HttpUtils.MethodConstants.PUT -> {
                    byte[] newEntityValue = http.getRequestBody().readAllBytes();
                    dao.upsert(entityId, newEntityValue);
                    sendResponse(http, HttpUtils.StatusConstants.CREATED);
                }
                case HttpUtils.MethodConstants.DELETE -> {
                    dao.delete(entityId);
                    sendResponse(http, HttpUtils.StatusConstants.ACCEPTED);
                }
                default -> sendResponse(http, HttpUtils.StatusConstants.METHOD_NOT_ALLOWED);
            }

            http.close();
        }));
    }

    @Override
    public void start() {
        server.start();
        log.info("Server has started.");
    }

    @Override
    public void stop() {
        server.stop(SERVER_STOP_TIMEOUT_SEC);
        log.info("Server has stopped.");
    }

    private record ErrorHttpHandler(HttpHandler delegate) implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (HttpUtils.AutoCloseableHttpExchange wrapped = new HttpUtils.AutoCloseableHttpExchange(exchange)) {
                HttpExchange wrappedExchange = wrapped.get();
                try {
                    delegate.handle(wrappedExchange);
                } catch (IllegalArgumentException e) {
                    sendResponse(wrappedExchange, HttpUtils.StatusConstants.BAD_REQUEST);
                } catch (NoSuchElementException e) {
                    sendResponse(wrappedExchange, HttpUtils.StatusConstants.NOT_FOUND);
                } catch (IOException e) {
                    sendResponse(wrappedExchange, HttpUtils.StatusConstants.INTERNAL_SERVER_ERROR);
                } catch (Exception e) {
                    sendResponse(wrappedExchange, HttpUtils.StatusConstants.SERVICE_UNAVAILABLE);
                }
            }
        }
    }
}
