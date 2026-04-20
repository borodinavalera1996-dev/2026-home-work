package company.vk.edu.distrib.compute.aldor7705.handler;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.aldor7705.exceptions.MethodNotAllowedException;

import java.io.IOException;
import java.util.Objects;

public class StatusHandler extends BaseHttpHandler {

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (!Objects.equals(method, "GET")) {
            throw new MethodNotAllowedException("Метод " + exchange.getRequestMethod()
                    + " не поддерживается для /status");
        }
        sendEmptyAnswer(exchange, 200);
    }
}
