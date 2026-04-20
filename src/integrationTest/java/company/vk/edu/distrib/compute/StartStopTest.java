package company.vk.edu.distrib.compute;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Basic init/deinit test for {@link KVService} implementation.
 *
 */
@ParameterizedClass
@ArgumentsSource(KVServiceFactoryArgumentsProvider.class)
class StartStopTest extends TestBase {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Parameter
    KVServiceFactory kvServiceFactory;

    @AfterAll
    public static void afterAll() {
        HTTP_CLIENT.close();
    }

    private static int status(int port) throws IOException, URISyntaxException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI("http://localhost:" + port + "/v0/status"))
            .timeout(Duration.ofSeconds(2))
            .build();
        HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode();
    }

    @Test
    void create() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            kvServiceFactory.create(port);
            assertThrows(IOException.class, () -> status(port));
        });
    }

    @Test
    void start() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            KVService storage = kvServiceFactory.create(port);
            try {
                storage.start();
                assertEquals(200, status(port));
            } finally {
                storage.stop();
            }
        });
    }

    @Test
    void stop() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            int port = randomPort();
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            storage.stop();
            assertThrows(IOException.class, () -> status(port));
        });
    }

    @Override
    protected HttpClient getHttpClient() {
        return HTTP_CLIENT;
    }
}
