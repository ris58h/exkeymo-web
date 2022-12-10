package ris58h.exkeymo.web;

import ch.cern.test.mdm.utils.SignedJar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApkBuilder {
    private static final Logger log = LoggerFactory.getLogger(ApkBuilder.class);

    private static final String KEYBOARD_LAYOUT_FILE_NAME = "res/Q2.kcm";
    private static final String KEYBOARD_LAYOUT2_FILE_NAME = "res/_f.kcm";

    private final char[] keystorePassword;

    private byte[] inAppBytes;
    private byte[] inApp2Bytes;
    private X509Certificate certificate;
    private PrivateKey privateKey;

    public ApkBuilder(String keystorePassword) {
        this.keystorePassword = keystorePassword.toCharArray();
    }

    public void init() throws Exception {
        this.inAppBytes = Resources.readAllBytesUnsafe("/app-oneLayout-release-unsigned.apk");
        this.inApp2Bytes = Resources.readAllBytesUnsafe("/app-twoLayouts-release-unsigned.apk");

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(Resources.getAsStream("/exkeymo.keystore"), keystorePassword);
        this.certificate = (X509Certificate) keyStore.getCertificate("app");
        this.privateKey = (PrivateKey) keyStore.getKey("app", keystorePassword);

        Arrays.fill(keystorePassword, (char) 0);
    }

    public byte[] buildApp(String layout, String layout2) throws Exception {
        try (ByteArrayOutputStream outAppBytes = new ByteArrayOutputStream(inAppBytes.length)) {
            log.info("Building app...");
            long start = System.currentTimeMillis();

            buildApp(layout, layout2, outAppBytes);

            if (log.isInfoEnabled()) {
                long mills = System.currentTimeMillis() - start;
                log.info(String.format("App is built in %.1f seconds", (((double) mills) / 1000)));
            }

            return outAppBytes.toByteArray();
        }
    }

    private void buildApp(String layout, String layout2, OutputStream outAppStream) throws Exception {
        byte[] zipBytes = layout2 == null ? inAppBytes : inApp2Bytes;
        ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
        SignedJar signedJar = new SignedJar(
                outAppStream,
                Collections.singleton(certificate),
                certificate,
                privateKey
        );
        ZipEntry zipEntry;
        while ((zipEntry = zipStream.getNextEntry()) != null) {
            if (zipEntry.isDirectory()) {
                continue;
            }
            String name = zipEntry.getName();
            final byte[] bytes = switch (name) {
                case KEYBOARD_LAYOUT_FILE_NAME -> layout.getBytes(StandardCharsets.UTF_8);
                case KEYBOARD_LAYOUT2_FILE_NAME -> layout2 == null ? null : layout2.getBytes(StandardCharsets.UTF_8);
                default -> zipStream.readAllBytes();
            };
            if (bytes != null) {
                signedJar.addFileContents(name, bytes);
            }
        }
        signedJar.close();
    }
}
