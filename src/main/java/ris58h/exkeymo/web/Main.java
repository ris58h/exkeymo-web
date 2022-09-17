package ris58h.exkeymo.web;

import java.util.Optional;

public class Main {
    public static void main(String[] args) throws Exception {
        ApkBuilder appBuilder = new ApkBuilder(
                stringProperty("keystore.password")
                        .orElseThrow(() -> new RuntimeException("Property 'keystore.password' is not specified"))
        );
        appBuilder.init();

        Server server = new Server(
                integerProperty("server.port").orElse(80),
                integerProperty("server.threads").orElse(Runtime.getRuntime().availableProcessors()),
                appBuilder
        );
        server.init();
    }

    private static Optional<Integer> integerProperty(String key) {
        return stringProperty(key).map(Integer::valueOf);
    }

    private static Optional<String> stringProperty(String key) {
        return Optional.ofNullable(System.getenv(envPropertyKey(key)))
                .or(() -> Optional.ofNullable(System.getProperty(key)));
    }

    private static String envPropertyKey(String propertyKey) {
        return propertyKey.toUpperCase().replace('.', '_');
    }
}
