package util;

import lombok.experimental.UtilityClass;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.util.encoders.Base64;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyPair;

import static java.nio.charset.Charset.defaultCharset;

@UtilityClass
public class EncryptionUtils {

    public static KeyPair extractKeyPair(final File encodedPk, final String passphrase) throws IOException {
        PEMEncryptedKeyPair pemEncryptedKeyPair = (PEMEncryptedKeyPair) readObject(encodedPk);
        if (pemEncryptedKeyPair == null) {
            throw new RuntimeException("Can't parse PK");
        }
        PEMDecryptorProvider pemDecryptorProvider = new JcePEMDecryptorProviderBuilder().build(passphrase.toCharArray());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        PEMKeyPair keyPair = pemEncryptedKeyPair.decryptKeyPair(pemDecryptorProvider);
        return converter.getKeyPair(keyPair);
    }

    private static Object readObject(File encodedPk) throws IOException {
        String key = readFile(encodedPk);
        String decodedKey = decodeKey(key);
        try (PEMParser pemParser = new PEMParser(new StringReader(decodedKey))) {
            return pemParser.readObject();
        }
    }

    private String decodeKey(String encodedKey) {
        return new String(Base64.decode(encodedKey));
    }

    private String readFile(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return IOUtils.toString(inputStream, defaultCharset());
        }
    }
}
