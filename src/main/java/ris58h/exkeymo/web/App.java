package ris58h.exkeymo.web;

public class App {
    public static void main(String[] args) throws Exception {
        int port = intSystemProperty("server.port", 80);
        int threads = intSystemProperty("server.threads", Runtime.getRuntime().availableProcessors());

        ApkBuilder appBuilder = new ApkBuilder();
        appBuilder.init();

        Server server = new Server(port, threads, appBuilder);
        server.init();
    }

    private static int intSystemProperty(String key, int defaultValue) {
        String property = System.getProperty(key);
        return property != null ? Integer.parseInt(property) : defaultValue;
    }
}
