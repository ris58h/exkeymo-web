package ris58h.exkeymo.web;

import ch.cern.test.mdm.utils.SignedJar;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AppBuilder {
    private byte[] inAppBytes;
    private X509Certificate certificate;
    private PrivateKey privateKey;

    public void init() throws Exception {
        InputStream unsignedAppStream = AppBuilder.class.getResourceAsStream("/app-release-unsigned.apk");
        this.inAppBytes = unsignedAppStream.readAllBytes();

        KeyStore keyStore = KeyStore.getInstance("JKS");
        char[] password = "exkeymo".toCharArray();
        keyStore.load(AppBuilder.class.getResourceAsStream("/exkeymo.keystore"), password);
        this.certificate = (X509Certificate) keyStore.getCertificate("app");
        privateKey = (PrivateKey) keyStore.getKey("app", password);
    }

    public byte[] buildApp(String layout) throws Exception {
        ByteArrayOutputStream outAppBytes = new ByteArrayOutputStream(inAppBytes.length);
        buildApp(layout, outAppBytes);
        return outAppBytes.toByteArray();
    }

    private void buildApp(String layout, OutputStream outAppStream) throws Exception {
        ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(inAppBytes));
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
            if (name.equals("META-INF/MANIFEST.MF")) {
                continue;
            }
            byte[] bytes;
            if (name.equals("res/raw/keyboard_layout.kcm")) {
                bytes = layout.getBytes(StandardCharsets.UTF_8);
            } else {
                bytes = zipStream.readAllBytes();
            }
            signedJar.addFileContents(name, bytes);
        }
        signedJar.close();
    }
}
