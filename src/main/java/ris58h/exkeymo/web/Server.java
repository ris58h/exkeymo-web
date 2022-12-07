package ris58h.exkeymo.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final int port;
    private final int threads;
    private final ApkBuilder apkBuilder;

    public Server(int port, int threads, ApkBuilder appBuilder) {
        this.port = port;
        this.threads = threads;
        this.apkBuilder = appBuilder;
    }

    public void init() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(threads));
        server.createContext("/", this::handleRoot);
        server.createContext("/simple", this::handleSimple);
        server.createContext("/complex", this::handleComplex);
        server.createContext("/docs", this::handleDocs);
        server.createContext("/common.css", exchange -> doGetCss(exchange, "/common.css"));

        server.start();
        log.info("Server started at port " + port);
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "/simple");
        exchange.sendResponseHeaders(302, -1);
    }

    private void handleGetPost(HttpExchange exchange, HttpHandler getHandler, HttpHandler postHandler) throws IOException {
        String requestMethod = exchange.getRequestMethod();

        if ("GET".equals(requestMethod)) {
            getHandler.handle(exchange);
            return;
        }

        if ("POST".equals(requestMethod)) {
            postHandler.handle(exchange);
            return;
        }

        exchange.sendResponseHeaders(404, -1);
    }

    private void handleSimple(HttpExchange exchange) throws IOException {
        handleGetPost(exchange, this::doSimpleGet, this::doSimplePost);
    }

    private void handleComplex(HttpExchange exchange) throws IOException {
        handleGetPost(exchange, this::doComplexGet, this::doComplexPost);
    }

    private void handleDocs(HttpExchange exchange) throws IOException {
        doGetText(exchange, "/docs.html");
    }

    private void doSimpleGet(HttpExchange exchange) throws IOException {
        doGetText(exchange, "/simple.html");
    }

    private void doSimplePost(HttpExchange exchange) throws IOException {
        doPost(exchange, params -> {
            String layout = null;
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<String, String> e : params.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();
                if (key.startsWith("from")) {
                    String keyCode = params.get("to" + key.substring(4));
                    if (keyCode == null) {
                        continue;
                    }
                    map.put(value, keyCode);
                } else if (key.equals("layout") && !value.isEmpty()) {
                    layout = readLayout(value);
                }
            }

            if (layout == null) {
                layout = "type OVERLAY\n";
            } else {
                //TODO: check for duplicate mappings (some keys could be already remapped in KCM-file)
            }

            if (map.isEmpty()) {
                return List.of(layout);
            }

            StringBuilder sb = new StringBuilder(layout);
            sb.append("\n# Modifications made by ExKeyMo project:\n");
            for (Map.Entry<String, String> e : map.entrySet()) {
                String code = e.getKey();
                String keyCode = e.getValue();
                sb.append("map key ").append(code).append(' ').append(keyCode).append('\n');
            }
            //TODO: multiple layouts
            return List.of(sb.toString());
        });
    }

    private static String readLayout(String name) {
        byte[] bytes = Resources.readAllBytesSafe("/kcm/" + name);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private void doComplexGet(HttpExchange exchange) throws IOException {
        doGetText(exchange, "/complex.html");
    }

    private void doComplexPost(HttpExchange exchange) throws IOException {
        doPost(exchange, params -> {
            String layout = params.get("layout");
            if (layout == null) {
                return List.of();
            }
            String layout2 = params.get("layout2");
            if (layout2 == null) {
                return List.of(layout);
            }
            return List.of(layout, layout2);
        });
    }

    private static void doGetText(HttpExchange exchange, String resourcePath) throws IOException {
        doGet(exchange, resourcePath, "text/html");
    }

    private static void doGetCss(HttpExchange exchange, String resourcePath) throws IOException {
        doGet(exchange, resourcePath, "text/css");
    }

    private static void doGet(HttpExchange exchange, String resourcePath, String contentType) throws IOException {
        byte[] htmlBytes = Resources.readAllBytesSafe("/public" + resourcePath);

        if (htmlBytes == null) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, htmlBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(htmlBytes);
        os.close();
    }

    private void doPost(HttpExchange exchange, Function<Map<String, String>, List<String>> paramsToLayouts) throws IOException {
        Map<String, String> params = parseParams(new String(exchange.getRequestBody().readAllBytes()));

        List<String> layouts = paramsToLayouts.apply(params);

        if (layouts.isEmpty() || layouts.size() > 2) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        String layout = layouts.get(0);
        String layout2 = layouts.size() > 1 ? layouts.get(1) : null;

        byte[] bytes;
        try {
            bytes = apkBuilder.buildApp(layout, layout2);
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
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"ExKeyMo Keyboard Layout.apk\"");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static Map<String, String> parseParams(String request) {
        String[] keyValues = request.split("&");
        Map<String, String> params = new HashMap<>();
        for (String keyValue : keyValues) {
            int i = keyValue.indexOf('=');
            if (i == -1) {
                continue;
            }
            String key = URLDecoder.decode(keyValue.substring(0, i), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(keyValue.substring(i + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }
}
