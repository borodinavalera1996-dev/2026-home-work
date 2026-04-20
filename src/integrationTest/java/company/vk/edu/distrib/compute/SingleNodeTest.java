package company.vk.edu.distrib.compute;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@ParameterizedClass
@ArgumentsSource(KVServiceFactoryArgumentsProvider.class)
class SingleNodeTest extends TestBase {

    static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Parameter
    KVServiceFactory kvServiceFactory;

    @AfterAll
    static void afterAll() {
        HTTP_CLIENT.close();
    }

    @Test
    void emptyKey() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                assertEquals(400, get(endpoint, "").statusCode());
                assertEquals(400, delete(endpoint, "").statusCode());
                assertEquals(400, upsert(endpoint, "", new byte[]{0}).statusCode());
            } finally {
                storage.stop();
            }
        });
    }

    @Test
    void badRequest() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(new URI(url(endpoint, "/abracadabra")))
                    .timeout(TIMEOUT)
                    .build();
                assertEquals(404, HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
            } finally {
                storage.stop();
            }
        });
    }

    @Test
    void getAbsent() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                assertEquals(404, get(endpoint, "absent").statusCode());
            } finally {
                storage.stop();
            }
        });
    }

    @Test
    void deleteAbsent() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                assertEquals(202, delete(endpoint, "absent").statusCode());
            } finally {
                storage.stop();
            }
        });
    }

    @Test
    void insert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                String key = randomKey();
                byte[] value = randomValue();

                assertEquals(201, upsert(endpoint, key, value).statusCode());

                final HttpResponse<byte[]> response = get(endpoint, key);
                assertEquals(200, response.statusCode());
                assertArrayEquals(value, response.body());
            } finally {
                storage.stop();
            }
        });
    }

    @Test
    void insertEmpty() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                final String key = randomKey();
                final byte[] value = new byte[0];

                assertEquals(201, upsert(endpoint, key, value).statusCode());

                final HttpResponse<byte[]> response = get(endpoint, key);
                assertEquals(200, response.statusCode());
                assertArrayEquals(value, response.body());
            } finally {
                storage.stop();
            }
        });
    }

    @Test
    void lifecycle2keys() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                final String key1 = randomKey();
                final byte[] value1 = randomValue();
                final String key2 = randomKey();
                final byte[] value2 = randomValue();

                assertEquals(201, upsert(endpoint, key1, value1).statusCode());

                assertArrayEquals(value1, get(endpoint, key1).body());

                assertEquals(201, upsert(endpoint, key2, value2).statusCode());

                assertArrayEquals(value1, get(endpoint, key1).body());
                assertArrayEquals(value2, get(endpoint, key2).body());

                assertEquals(202, delete(endpoint, key1).statusCode());

                assertEquals(404, get(endpoint, key1).statusCode());
                assertArrayEquals(value2, get(endpoint, key2).body());

                assertEquals(202, delete(endpoint, key2).statusCode());

                assertEquals(404, get(endpoint, key2).statusCode());
            } finally {
                storage.stop();
            }
        });
    }

    @Test
    void upsert() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                final String key = randomKey();
                final byte[] value1 = randomValue();
                final byte[] value2 = randomValue();

                assertEquals(201, upsert(endpoint, key, value1).statusCode());

                assertEquals(201, upsert(endpoint, key, value2).statusCode());

                final HttpResponse<byte[]> response = get(endpoint, key);
                assertEquals(200, response.statusCode());
                assertArrayEquals(value2, response.body());
            } finally {
                storage.stop();
            }
        });
    }

    @Test
    void upsertEmpty() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                final String key = randomKey();
                final byte[] value = randomValue();
                final byte[] empty = new byte[0];

                assertEquals(201, upsert(endpoint, key, value).statusCode());

                assertEquals(201, upsert(endpoint, key, empty).statusCode());

                final HttpResponse<byte[]> response = get(endpoint, key);
                assertEquals(200, response.statusCode());
                assertArrayEquals(empty, response.body());
            } finally {
                storage.stop();
            }
        });
    }

    @Test
    void delete() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                final String key = randomKey();
                final byte[] value = randomValue();

                assertEquals(201, upsert(endpoint, key, value).statusCode());

                assertEquals(202, delete(endpoint, key).statusCode());

                assertEquals(404, get(endpoint, key).statusCode());
            } finally {
                storage.stop();
            }
        });
    }

    @Override
    protected HttpClient getHttpClient() {
        return HTTP_CLIENT;
    }
}
