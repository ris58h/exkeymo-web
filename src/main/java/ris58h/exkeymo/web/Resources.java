package ris58h.exkeymo.web;

import java.io.IOException;
import java.io.InputStream;

public class Resources {
    public static byte[] readAllBytes(Class<?> clazz, String name) throws IOException {
        try (InputStream is = clazz.getResourceAsStream(name)) {
            return is == null ? null : is.readAllBytes();
        }
    }
}
