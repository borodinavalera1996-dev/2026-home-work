package company.vk.edu.distrib.compute.aldor7705.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.aldor7705.exceptions.MethodNotAllowedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public abstract class BaseHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            handleRequest(exchange);
        } catch (MethodNotAllowedException e) {
            sendError(exchange, 405, e.getMessage());
        } catch (NoSuchElementException e) {
            sendError(exchange, 404, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            sendError(exchange,500, "Непредвиденная ошибка на сервере: " + e.getMessage());
        }
    }

    protected abstract void handleRequest(HttpExchange exchange) throws IOException;

    protected void sendError(HttpExchange h, int code, String text) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        h.sendResponseHeaders(code, resp.length);
        try (var outputStream = h.getResponseBody()) {
            outputStream.write(resp);
        }
    }

    protected void sendAnswer(HttpExchange h, byte[] resp, int code) throws IOException {
        h.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        h.sendResponseHeaders(code, resp.length);
        try (var outputStream = h.getResponseBody()) {
            outputStream.write(resp);
        }
    }

    protected void sendEmptyAnswer(HttpExchange h, int code) throws IOException {
        h.sendResponseHeaders(code, 0);
        h.getResponseBody().close();
    }
}
