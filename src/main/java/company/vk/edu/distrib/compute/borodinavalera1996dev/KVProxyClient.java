package company.vk.edu.distrib.compute.borodinavalera1996dev;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.borodinavalera1996dev.cluster.Node;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class KVProxyClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void proxy(HttpExchange exchange, Node targetNode) throws IOException {
        String method = exchange.getRequestMethod();
        String uri = targetNode.getName() + exchange.getRequestURI().getPath() + "?"
                + exchange.getRequestURI().getQuery();
        HttpRequest.Builder request = HttpRequest.newBuilder().uri(URI.create(uri));

        if ("PUT".equals(method)) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            request.PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        } else if ("GET".equals(method)) {
            request.GET();
        } else if ("DELETE".equals(method)) {
            request.DELETE();
        }

        try {
            HttpResponse<byte[]> response = httpClient.send(
                    request.build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            exchange.sendResponseHeaders(response.statusCode(), response.body().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exchange.sendResponseHeaders(500, -1);
        }
    }
}
