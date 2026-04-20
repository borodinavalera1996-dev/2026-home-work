package company.vk.edu.distrib.compute.aldor7705.handler;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.aldor7705.exceptions.MethodNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EntityHandler extends BaseHttpHandler {
    private static final Logger log = LoggerFactory.getLogger(EntityHandler.class);

    private final Dao<byte[]> dao;

    public EntityHandler(Dao<byte[]> dao) {
        super();
        this.dao = dao;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI().getQuery();
        String id = getIdFromQuery(query);

        switch (method) {
            case "GET":
                byte[] data = getEntityDao(id);
                sendAnswer(exchange, data, 200);
                break;
            case "PUT":
                byte[] bytes = exchange.getRequestBody().readAllBytes();
                dao.upsert(id, bytes);
                sendEmptyAnswer(exchange, 201);
                break;
            case "DELETE":
                dao.delete(id);
                sendEmptyAnswer(exchange, 202);
                break;
            default:
                throw new MethodNotAllowedException("Метод " + method + " не поддерживается для /entity");
        }
    }

    private String getIdFromQuery(String query) {
        if (query == null || !query.startsWith("id=")) {
            log.warn("Ошибка при попытке получить id");
            throw new IllegalArgumentException("id отсутствует");
        }
        String id = query.substring(3);
        if (id.isEmpty()) {
            log.warn("Ошибка при попытке получить id");
            throw new IllegalArgumentException("id пуст");
        }
        return id;
    }

    private byte[] getEntityDao(String id) throws IOException {
        return dao.get(id);
    }
}
