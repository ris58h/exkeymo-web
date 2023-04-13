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
    private static final String PUBLIC_RESOURCES_PATH = "/public";

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
        server.start();
        log.info("Server started at port " + port);
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        switch (path) {
            case "/" -> serveRedirect(exchange, "/simple");
            case "/simple" -> handleGetPost(exchange, e -> servePublicResource(e, "/simple.html"), this::doPostSimple);
            case "/complex" -> handleGetPost(exchange, e -> servePublicResource(e, "/complex.html"), this::doPostComplex);
            case "/docs" -> servePublicResource(exchange, "/docs.html");
            default -> {
                if (Resources.exists(PUBLIC_RESOURCES_PATH + path)) {
                    servePublicResource(exchange, path);
                } else {
                    serveNotFound(exchange);
                }
            }
        }
    }

    private static void handleGetPost(HttpExchange exchange, HttpHandler getHandler, HttpHandler postHandler) throws IOException {
        String requestMethod = exchange.getRequestMethod();

        if ("GET".equals(requestMethod)) {
            getHandler.handle(exchange);
            return;
        }

        if ("POST".equals(requestMethod)) {
            postHandler.handle(exchange);
            return;
        }

        serveNotFound(exchange);
    }

    private record Response(int code, Map<String, String> headers, byte[] body) {
        public Response(int code) {
            this(code, null, null);
        }

        public Response(int code, Map<String, String> headers) {
            this(code, headers, null);
        }

        public Response(int code, String body) {
            this(code, null, body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void serveResponse(HttpExchange exchange, Response response) throws IOException {
        if (response.headers != null) {
            response.headers.forEach((key, value) -> {
                exchange.getResponseHeaders().set(key, value);
            });
        }
        if (response.body == null) {
            exchange.sendResponseHeaders(response.code, -1);
        } else {
            exchange.sendResponseHeaders(response.code, response.body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.body);
            }
        }
    }

    private static void serveRedirect(HttpExchange exchange, String path) throws IOException {
        serveResponse(exchange, new Response(
                302,
                Map.of("Location", path)
        ));
    }

    private static void serveBadRequest(HttpExchange exchange) throws IOException {
        serveResponse(exchange, new Response(400));
    }

    private static void serveNotFound(HttpExchange exchange) throws IOException {
        serveResponse(exchange, new Response(404));
    }

    private static void serveError(HttpExchange exchange, String body) throws IOException {
        serveResponse(exchange, new Response(500, body));
    }

    private static void servePublicResource(HttpExchange exchange, String path) throws IOException {
        if (path.endsWith(".html")) {
            servePublicResource(exchange, path, "text/html");
        } else if (path.endsWith(".css")) {
            servePublicResource(exchange, path, "text/css");
        } else {
            servePublicResource(exchange, path, "text/plain");
        }
    }

    private void doPostSimple(HttpExchange exchange) throws IOException {
        doPost(exchange, params -> {
            String layoutName = null;
            String layout2Name = null;
            Map<String, String> mappings = new HashMap<>();
            for (Map.Entry<String, String> e : params.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();
                if (key.startsWith("from")) {
                    if (value.isEmpty()) {
                        continue;
                    }
                    String keyCode = params.get("to" + key.substring(4));
                    if (keyCode == null || keyCode.isEmpty()) {
                        continue;
                    }
                    mappings.put(value, keyCode);
                } else if (key.equals("layout")) {
                    layoutName = value;
                } else if (key.equals("layout2")) {
                    layout2Name = value;
                }
            }

            if (layout2Name == null || layout2Name.equals("-")) {
                String layout = Layouts.fromNamedLayout(layoutName, mappings);
                return List.of(layout);
            } else {
                String layout = Layouts.fromNamedLayout(layoutName, mappings);
                String layout2 = Layouts.fromNamedLayout(layout2Name, mappings);
                return List.of(layout, layout2);
            }
        });
    }

    private void doPostComplex(HttpExchange exchange) throws IOException {
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

    private static void servePublicResource(HttpExchange exchange, String path, String contentType) throws IOException {
        byte[] htmlBytes = Resources.readAllBytesSafe(PUBLIC_RESOURCES_PATH + path);

        if (htmlBytes == null) {
            serveNotFound(exchange);
            return;
        }

        serveResponse(exchange, new Response(
                200,
                Map.of("Content-Type", contentType),
                htmlBytes
        ));
    }

    private void doPost(HttpExchange exchange, Function<Map<String, String>, List<String>> paramsToLayouts) throws IOException {
        Map<String, String> params = parseParams(new String(exchange.getRequestBody().readAllBytes()));

        List<String> layouts = paramsToLayouts.apply(params);

        if (layouts.isEmpty() || layouts.size() > 2) {
            serveBadRequest(exchange);
            return;
        }

        String layout = layouts.get(0);
        String layout2 = layouts.size() > 1 ? layouts.get(1) : null;

        byte[] bytes;
        try {
            bytes = apkBuilder.buildApp(layout, layout2);
        } catch (Exception e) {
            log.error("Error while building app", e);
            serveError(exchange, e.getMessage());
            return;
        }

        serveResponse(exchange, new Response(
                200,
                Map.of(
                        "Content-Type", "application/vnd.android.package-archive",
                        "Content-Disposition", "attachment; filename=\"ExKeyMo Keyboard Layout.apk\""
                ),
                bytes
        ));
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
