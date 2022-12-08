package ris58h.exkeymo.web;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

public class Layouts {
    private static final String DEFAULT = "type OVERLAY\n";
    private static final String MODIFICATIONS_TRAILING_COMMENT = "# Modifications made by ExKeyMo project:\n";

    public static String fromNamedLayout(String baseLayoutName, Map<String, String> mappings) {
        String layout = null;
        if (baseLayoutName != null) {
            layout = readLayout(baseLayoutName);
        }
        if (layout == null) {
            layout = Layouts.DEFAULT;
        }

        return fromLayout(layout, mappings);
    }

    private static String fromLayout(String layout, Map<String, String> mappings) {
        if (mappings.isEmpty()) {
            return layout;
        }

        StringBuilder sb = new StringBuilder();

        try (Scanner scanner = new Scanner(layout)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                MapKey mapKey = parseMapKey(line);
                if (mapKey != null) {
                    String code = (mapKey.usage ? "usage " : "") + mapKey.code;
                    if (mappings.containsKey(code)) {
                        sb.append(MODIFICATIONS_TRAILING_COMMENT);
                        sb.append("# ");
                    }
                }
                sb.append(line).append('\n');
            }
        }
        sb.append('\n').append(MODIFICATIONS_TRAILING_COMMENT);
        for (Map.Entry<String, String> e : mappings.entrySet()) {
            String code = e.getKey();
            String keyCode = e.getValue();
            sb.append("map key ").append(code).append(' ').append(keyCode).append('\n');
        }
        return sb.toString();
    }

    private static MapKey parseMapKey(String line) {
        String trimmedLine = line.trim();
        if (!trimmedLine.startsWith("map ")) {
            return null;
        }
        String[] split = line.split(" ");
        if (split.length < 4) {
            return null;
        }
        if (!"key".equals(split[1])) {
            return null;
        }
        if ("usage".equals(split[2])) {
            if (split.length != 5) {
                return null;
            }
            String code = split[3];
            String keyCode = split[4];
            return new MapKey(true, code, keyCode);
        } else {
            if (split.length != 4) {
                return null;
            }
            String code = split[2];
            String keyCode = split[3];
            return new MapKey(false, code, keyCode);
        }
    }

    private static class MapKey {
        public final boolean usage;
        public final String code;
        public final String keyCode;

        private MapKey(boolean usage, String code, String keyCode) {
            this.usage = usage;
            this.code = code;
            this.keyCode = keyCode;
        }
    }

    private static String readLayout(String name) {
        byte[] bytes = Resources.readAllBytesSafe("/kcm/" + name);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }
}
