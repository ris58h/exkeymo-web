package ris58h.exkeymo.web;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class App {
    public static void main(String[] args) throws Exception {
        //TODO
        int port = 80;
        int threads = 2;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(threads));

        byte[] indexHtmlBytes = App.class.getResourceAsStream("/index.html").readAllBytes();
        server.createContext("/", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, indexHtmlBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(indexHtmlBytes);
            os.close();
        });

        AppBuilder appBuilder = new AppBuilder();
        appBuilder.init();
        server.createContext("/build", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String layout = parseLayoutFromRequest(new String(exchange.getRequestBody().readAllBytes()));
            if (layout == null) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            byte[] bytes;
            try {
                bytes = appBuilder.buildApp(layout);
            } catch (Exception e) {
                bytes = ("Processing error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
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
        });

        server.start();
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
}
