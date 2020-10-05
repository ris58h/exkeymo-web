package ris58h.exkeymo.web;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        int port = intSystemProperty("server.port", 80);
        int threads = intSystemProperty("server.threads", Runtime.getRuntime().availableProcessors());

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(threads));

        byte[] htmlBytes = App.class.getResourceAsStream("/index.html").readAllBytes();
        AppBuilder appBuilder = new AppBuilder();
        appBuilder.init();
        server.createContext("/", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, htmlBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(htmlBytes);
                os.close();
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                String layout = parseLayoutFromRequest(new String(exchange.getRequestBody().readAllBytes()));
                if (layout == null) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }

                byte[] bytes;
                try {
                    log.info("Building app...");
                    long start = System.currentTimeMillis();
                    bytes = appBuilder.buildApp(layout);
                    if (log.isInfoEnabled()) {
                        long mills = System.currentTimeMillis() - start;
                        log.info(String.format("App is built in %.1f seconds", (((double) mills) / 1000)));
                    }
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
                return;
            }

            exchange.sendResponseHeaders(404, -1);
        });

        server.start();
        log.info("Server started at port " + port);
    }

    private static String parseLayoutFromRequest(String request) {
        if (request.startsWith("layout=")) {
            try {
                return URLDecoder.decode(request.substring("layout=".length()), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return null;
    }

    private static int intSystemProperty(String key, int defaultValue) {
        String property = System.getProperty(key);
        return property != null ? Integer.parseInt(property) : defaultValue;
    }
}
