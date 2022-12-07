package ris58h.exkeymo.web;

import java.io.IOException;
import java.io.InputStream;

public class Resources {
    public static byte[] readAllBytesSafe(String path) {
        if (path.isEmpty() || path.equals("..") || path.contains("../")) {
            return null;
        }
        try {
            return readAllBytesUnsafe(path);
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] readAllBytesUnsafe(String path) throws IOException {
        try (InputStream is = Resources.class.getResourceAsStream(path)) {
            return is == null ? null : is.readAllBytes();
        }
    }
}
