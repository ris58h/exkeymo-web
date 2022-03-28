package ris58h.exkeymo.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final int port;
    private final int threads;
    private final ApkBuilder apkBuilder;

    byte[] htmlBytes;

    public Server(int port, int threads, ApkBuilder appBuilder) {
        this.port = port;
        this.threads = threads;
        this.apkBuilder = appBuilder;
    }

    public void init() throws Exception {
        this.htmlBytes = App.class.getResourceAsStream("/index.html").readAllBytes();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(threads));
        server.createContext("/", exchange -> {
            String requestMethod = exchange.getRequestMethod();

            if ("GET".equals(requestMethod)) {
                doGet(exchange);
                return;
            }

            if ("POST".equals(requestMethod)) {
                doPost(exchange);
                return;
            }

            exchange.sendResponseHeaders(404, -1);
        });

        server.start();
        log.info("Server started at port " + port);
    }

    private void doGet(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, htmlBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(htmlBytes);
        os.close();
    }

    private void doPost(HttpExchange exchange) throws IOException {
        String layout = parseLayoutFromRequest(new String(exchange.getRequestBody().readAllBytes()));
        if (layout == null) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        byte[] bytes;
        try {
            bytes = apkBuilder.buildApp(layout);
        } catch (Exception e) {
            log.error("Error while building app", e);
            bytes = e.getMessage().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/vnd.android.package-archive");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"Custom Keyboard Layout.apk\"");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String parseLayoutFromRequest(String request) {
        if (request.startsWith("layout=")) {
            return URLDecoder.decode(request.substring("layout=".length()), StandardCharsets.UTF_8);
        }
        return null;
    }
}
