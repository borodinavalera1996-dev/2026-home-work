package company.vk.edu.distrib.compute.aldor7705;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import company.vk.edu.distrib.compute.aldor7705.handler.EntityHandler;
import company.vk.edu.distrib.compute.aldor7705.handler.StatusHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class KVServiceSimple implements KVService {
    private static final Logger log = LoggerFactory.getLogger(KVServiceSimple.class);
    private final HttpServer httpServer;

    public KVServiceSimple(int port, Dao<byte[]> dao) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/v0/status", new StatusHandler());
        httpServer.createContext("/v0/entity", new EntityHandler(dao));
    }

    @Override
    public void start() {
        log.info("Запуск сервиса");
        httpServer.start();
    }

    @Override
    public void stop() {
        log.info("Остановка сервиса");
        httpServer.stop(0);
    }
}
