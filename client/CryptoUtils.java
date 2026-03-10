import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public class CryptoUtils {

    // ======================
    // KEY GENERATION
    // ======================

    public static KeyPair generateRSAKeys() throws Exception {

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        return generator.generateKeyPair();
    }

    // ======================
    // SAVE PRIVATE KEY
    // ======================

    public static void savePrivateKey(String username, PrivateKey key)
            throws Exception {

        byte[] encoded = key.getEncoded();

        Files.write(
                Paths.get(username + "_private.key"),
                encoded
        );
    }

    // ======================
    // LOAD PRIVATE KEY
    // ======================

    public static PrivateKey loadPrivateKey(String username)
            throws Exception {

        byte[] keyBytes =
                Files.readAllBytes(
                        Paths.get(username + "_private.key")
                );

        PKCS8EncodedKeySpec spec =
                new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory factory =
                KeyFactory.getInstance("RSA");

        return factory.generatePrivate(spec);
    }

    // ======================
    // SERIALIZE PUBLIC KEY
    // ======================

    public static String serializePublicKey(PublicKey key) {

        return Base64.getEncoder()
                .encodeToString(key.getEncoded());
    }

    public static PublicKey deserializePublicKey(String str)
            throws Exception {

        byte[] bytes =
                Base64.getDecoder().decode(str);

        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(bytes);

        KeyFactory factory =
                KeyFactory.getInstance("RSA");

        return factory.generatePublic(spec);
    }

    // ======================
    // ENCRYPT MESSAGE
    // ======================

    public static EncryptedMessage encrypt(
            PublicKey publicKey,
            String message
    ) throws Exception {

        // AES key
        KeyGenerator keyGen =
                KeyGenerator.getInstance("AES");

        keyGen.init(128);

        SecretKey aesKey = keyGen.generateKey();

        // AES cipher
        Cipher aesCipher =
                Cipher.getInstance("AES/GCM/NoPadding");

        byte[] nonce = new byte[12];

        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);

        GCMParameterSpec spec =
                new GCMParameterSpec(128, nonce);

        aesCipher.init(
                Cipher.ENCRYPT_MODE,
                aesKey,
                spec
        );

        byte[] ciphertext =
                aesCipher.doFinal(
                        message.getBytes()
                );

        // RSA wrap AES key
        Cipher rsa =
                Cipher.getInstance(
                        "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
                );

        rsa.init(
                Cipher.ENCRYPT_MODE,
                publicKey
        );

        byte[] wrappedKey =
                rsa.doFinal(aesKey.getEncoded());

        return new EncryptedMessage(
                Base64.getEncoder().encodeToString(wrappedKey),
                Base64.getEncoder().encodeToString(nonce),
                Base64.getEncoder().encodeToString(ciphertext)
        );
    }

    // ======================
    // DECRYPT MESSAGE
    // ======================

    public static String decrypt(
            PrivateKey privateKey,
            EncryptedMessage msg
    ) throws Exception {

        byte[] wrappedKey =
                Base64.getDecoder().decode(msg.wrappedKey);

        byte[] nonce =
                Base64.getDecoder().decode(msg.nonce);

        byte[] ciphertext =
                Base64.getDecoder().decode(msg.ciphertext);

        Cipher rsa =
                Cipher.getInstance(
                        "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
                );

        rsa.init(
                Cipher.DECRYPT_MODE,
                privateKey
        );

        byte[] aesKeyBytes =
                rsa.doFinal(wrappedKey);

        SecretKey aesKey =
                new javax.crypto.spec.SecretKeySpec(
                        aesKeyBytes,
                        "AES"
                );

        Cipher aes =
                Cipher.getInstance("AES/GCM/NoPadding");

        GCMParameterSpec spec =
                new GCMParameterSpec(128, nonce);

        aes.init(
                Cipher.DECRYPT_MODE,
                aesKey,
                spec
        );

        byte[] plain =
                aes.doFinal(ciphertext);

        return new String(plain);
    }
}