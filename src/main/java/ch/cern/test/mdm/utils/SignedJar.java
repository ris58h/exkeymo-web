package ch.cern.test.mdm.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;

/**
 * Generator of signed Jars. It stores some data in memory therefore it is not
 * suited for creation of large files. TODO: add streaming interface for file
 * contents TODO: better error handling in #close() method
 * @author Michal Rydlo, Maciek Muszkowski
 * @see <a
 *      href="http://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html#Signed_JAR_File">JAR
 *      format specification</a>
 */
public class SignedJar {

    private static final String DIG_ALG = "SHA1";
    private static final String SIG_ALG = "SHA1withRSA";
    private static final String CREATED_BY = System.getProperty("java.version")
            + " (" + System.getProperty("java.vendor") + ")";
    private static final String SIG_FN = "META-INF/INTERMED.SF";
    private static final String SIG_RSA_FN = "META-INF/INTERMED.RSA";

    private final Collection<X509Certificate> mChain;
    private final X509Certificate mCert;
    private final PrivateKey mSignKey;
    private final MessageDigest mHashFunction;

    private final Map<String, String> mManifestAttributes;
    private final Map<String, String> mFileDigests;
    private final Map<String, String> mSectionDigests;
    private String mManifestHash;
    private String mManifestMainHash;

    private JarOutputStream mJarOut;

    /**
     * Constructor.
     * @param out
     *            the output stream to write JAR data to
     * @param chain
     *            certification chain
     * @param cert
     *            certificate included in signature
     * @param signKey
     *            key is used to sign the JAR
     * @throws NoSuchAlgorithmException
     *             on no such hashing algorithm
     * @throws IOException
     *             on JAR output stream creation failed
     * @throws NoSuchProviderException
     *             when BouncyCastle provider is unavailable
     */
    public SignedJar(OutputStream out,
                     Collection<X509Certificate> chain,
                     X509Certificate cert, PrivateKey signKey)
            throws NoSuchAlgorithmException, IOException {
        mJarOut = new JarOutputStream(out);
        mChain = chain;
        mCert = cert;
        mSignKey = signKey;
        mManifestAttributes = new LinkedHashMap<>();
        mFileDigests = new LinkedHashMap<>();
        mSectionDigests = new LinkedHashMap<>();
        mHashFunction = MessageDigest.getInstance(DIG_ALG);
    }

    /**
     * Adds a header to the manifest of the JAR.
     * @param name
     *            name of the attribute, it is placed into the main section of
     *            the manifest file, it cannot be longer than
     *            {@value #MANIFEST_ATTR_MAX_LEN} bytes (in utf-8 encoding)
     * @param value
     *            value of the attribute
     */
    public void addManifestAttribute(String name, String value) {
        mManifestAttributes.put(name, value);
    }

    /**
     * Adds a file to the JAR. The file is immediately added to the zipped
     * output stream. This method cannot be called once the stream is closed.
     * @param filename
     *            name of the file to add (use forward slash as a path
     *            separator)
     * @param contents
     *            contents of the file
     * @throws java.io.IOException
     * @throws NullPointerException
     *             if any of the arguments is {@code null}
     */
    public void addFileContents(String filename, byte[] contents)
            throws IOException {
        mJarOut.putNextEntry(new JarEntry(filename));
        mJarOut.write(contents);
        mJarOut.closeEntry();

        byte[] hashCode = mHashFunction.digest(contents);
        mFileDigests.put(filename, toBase64String(hashCode));
    }

    /**
     * Finishes the JAR file by writing the manifest and signature data to it
     * and finishing the ZIP entries. It leaves the underlying stream open.
     * @throws java.io.IOException
     * @throws RuntimeException
     *             if the signing goes wrong
     */
    public void finish() throws IOException {
        writeManifest();
        byte sig[] = writeSigFile();
        writeSignature(sig);
    }

    /**
     * Closes the JAR file by writing the manifest and signature data to it and
     * finishing the ZIP entries. It closes the underlying stream.
     * @throws java.io.IOException
     * @throws RuntimeException
     *             if the signing goes wrong
     */
    public void close() throws IOException {
        finish();
        mJarOut.close();
    }

    /** Creates the beast that can actually sign the data. */
    private CMSSignedDataGenerator createSignedDataGenerator() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        Store certStore = new JcaCertStore(mChain);
        ContentSigner signer = new JcaContentSignerBuilder(SIG_ALG)
                .setProvider("BC").build(mSignKey);
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        DigestCalculatorProvider dcp = new JcaDigestCalculatorProviderBuilder()
                .setProvider("BC").build();
        // jarsigner doesn't include attribute table
        SignerInfoGenerator sig = new JcaSignerInfoGeneratorBuilder(dcp)
                .setDirectSignature(true)
                .build(signer, mCert);

        generator.addSignerInfoGenerator(sig);
        generator.addCertificates(certStore);
        return generator;
    }

    /** Returns the CMS signed data. */
    private byte[] signSigFile(byte[] sigContents) throws Exception {
        CMSSignedDataGenerator gen = createSignedDataGenerator();
        CMSTypedData cmsData = new CMSProcessableByteArray(sigContents);
        CMSSignedData signedData = gen.generate(cmsData, false);

        // Android doesn't support indefinite length encoding
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(baos);
        ASN1InputStream aIn = new ASN1InputStream(signedData.getEncoded());
        dOut.writeObject(aIn.readObject());
        aIn.close();
        dOut.close();

        return baos.toByteArray();
    }

    /**
     * Signs the .SIG file and writes the signature (.RSA file) to the JAR.
     * @throws java.io.IOException
     * @throws RuntimeException
     *             if the signing failed
     */
    private void writeSignature(byte[] sigFile) throws IOException {
        mJarOut.putNextEntry(new JarEntry(SIG_RSA_FN));
        try {
            byte[] signature = signSigFile(sigFile);
            mJarOut.write(signature);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Signing failed.", e);
        } finally {
            mJarOut.closeEntry();
        }
    }

    /**
     * Writes the .SIG file to the JAR.
     * @return the contents of the file as bytes
     */
    private byte[] writeSigFile() throws IOException {
        mJarOut.putNextEntry(new JarEntry(SIG_FN));
        Manifest man = new Manifest();
        // main section
        Attributes mainAttributes = man.getMainAttributes();
        mainAttributes.put(Attributes.Name.SIGNATURE_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name("Created-By"), CREATED_BY);
        mainAttributes.put(new Attributes.Name(
                        DIG_ALG + "-Digest-Manifest"),
                mManifestHash);
        mainAttributes.put(new Attributes.Name(
                        DIG_ALG + "-Digest-Manifest-Main-Attributes"),
                mManifestMainHash);

        // individual files sections
        Attributes.Name digestAttr = new Attributes.Name(
                DIG_ALG + "-Digest");
        for (Map.Entry<String, String> entry : mSectionDigests.entrySet()) {
            Attributes attributes = new Attributes();
            man.getEntries().put(entry.getKey(), attributes);
            attributes.put(digestAttr, entry.getValue());
        }

        man.write(mJarOut);
        mJarOut.closeEntry();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        man.write(baos);
        return baos.toByteArray();
    }

    /**
     * Helper for {@link #writeManifest()} that creates the digest of one entry.
     */
    private String hashEntrySection(String name, Attributes attributes)
            throws IOException {
        // crate empty manifest
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(
                Attributes.Name.MANIFEST_VERSION, "1.0");
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        manifest.write(o);
        int emptyLen = o.toByteArray().length;

        // get hash of entry without manifest header
        manifest.getEntries().put(name, attributes);
        o.reset();
        manifest.write(o);
        byte[] ob = o.toByteArray();
        o.close();
        ob = Arrays.copyOfRange(ob, emptyLen, ob.length);
        return toBase64String(mHashFunction.digest(ob));
    }

    /**
     * Helper for {@link #writeManifest()} that creates the digest of the main
     * section.
     */
    private String hashMainSection(Attributes attributes) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putAll(attributes);
        return toBase64String(getManifestHash(manifest));
    }

    /**
     * Writes the manifest to the JAR. It also calculates the digests that are
     * required to be placed in the the signature file.
     * @throws java.io.IOException
     */
    private void writeManifest() throws IOException {
        mJarOut.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
        Manifest man = new Manifest();

        // main section
        Attributes mainAttributes = man.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name("Created-By"), CREATED_BY);

        for (Map.Entry<String, String> entry : mManifestAttributes.entrySet()) {
            mainAttributes.put(new Attributes.Name(entry.getKey()),
                    entry.getValue());
        }

        // individual files sections
        Attributes.Name digestAttr = new Attributes.Name(
                DIG_ALG + "-Digest");
        for (Map.Entry<String, String> entry : mFileDigests.entrySet()) {
            Attributes attributes = new Attributes();
            man.getEntries().put(entry.getKey(), attributes);
            attributes.put(digestAttr, entry.getValue());
            mSectionDigests.put(entry.getKey(),
                    hashEntrySection(entry.getKey(), attributes));
        }
        man.write(mJarOut);
        mJarOut.closeEntry();

        mManifestHash = toBase64String(getManifestHash(man));
        mManifestMainHash = hashMainSection(man.getMainAttributes());
    }

    /**
     * Returns the manifest hash.
     * @param manifest
     *            manifest
     * @return hash
     * @throws IOException
     *             on creating temporary byte buffer error
     */
    private byte[] getManifestHash(Manifest manifest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        manifest.write(baos);
        baos.close();
        return mHashFunction.digest(baos.toByteArray());
    }

    /**
     * Converts byte array to base64 string. I'm creating this method here
     * because there were some problems with importing BC Base64.toBase64String
     * on Android.
     * @param data
     *            byte array
     * @return base64 string
     */
    private static String toBase64String(byte[] data) {
        return new String(Base64.encode(data));
    }
}
