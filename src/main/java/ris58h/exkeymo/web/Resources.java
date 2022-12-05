package ris58h.exkeymo.web;

import java.io.IOException;
import java.io.InputStream;

public class Resources {
    public static byte[] readAllBytesSafe(String path) {
        if (path.equals("..") || path.contains("../")) {
            return null;
        }
        try {
            return readAllBytes(path);
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] readAllBytes(String path) throws IOException {
        try (InputStream is = Resources.class.getResourceAsStream(path)) {
            return is == null ? null : is.readAllBytes();
        }
    }
}
