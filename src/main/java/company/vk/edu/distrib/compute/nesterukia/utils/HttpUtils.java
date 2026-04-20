package company.vk.edu.distrib.compute.nesterukia.utils;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class HttpUtils {
    private static final int EMPTY_RESPONSE_LENGTH = 0;
    private static final String QUERY_PARAM_DELIMITER = "&";
    private static final String KEY_VALUE_DELIMITER = "=";

    public static final class MethodConstants {
        public static final String GET = "GET";
        public static final String POST = "POST";
        public static final String PUT = "PUT";
        public static final String DELETE = "DELETE";
    }

    public static final class StatusConstants {
        public static final int OK = 200;
        public static final int CREATED = 201;
        public static final int ACCEPTED = 202;
        public static final int BAD_REQUEST = 400;
        public static final int NOT_FOUND = 404;
        public static final int METHOD_NOT_ALLOWED = 405;
        public static final int INTERNAL_SERVER_ERROR = 500;
        public static final int SERVICE_UNAVAILABLE = 503;
    }

    public record AutoCloseableHttpExchange(HttpExchange exchange) implements AutoCloseable {
        public HttpExchange get() {
            return exchange;
        }

        @Override
        public void close() {
            exchange.close();
        }
    }

    public static void sendResponse(HttpExchange exchange, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, EMPTY_RESPONSE_LENGTH);
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, byte[] responseBody) throws IOException {
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }

    public static Map<String, String> parseQueryParams(HttpExchange http) {
        return Arrays.stream(http.getRequestURI()
                        .getQuery()
                        .split(QUERY_PARAM_DELIMITER))
                .map(param -> param.split(KEY_VALUE_DELIMITER))
                .filter(arr -> arr.length == 2)
                .collect(Collectors.toMap(
                        arr -> arr[0],
                        arr -> arr[1]
                ));
    }

    private HttpUtils() {
        // cannot have instances
    }
}
