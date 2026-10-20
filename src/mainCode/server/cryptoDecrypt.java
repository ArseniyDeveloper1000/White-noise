package mainCode.server;

import java.security.Security;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class cryptoDecrypt {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private String ALGORITHM;
    private static final byte[] SECRET_KEY = "MySuperSecretKey1234567890123456".getBytes();

    public cryptoDecrypt() {
        this.ALGORITHM = "AES-GCM";
    }

    public cryptoDecrypt(String algo) {
        this.ALGORITHM = algo;
    }

    public byte[] decrypt(byte[] encryptedData) throws Exception {
        switch (ALGORITHM) {
            case "AES-GCM":
                return decryptAesGcm(encryptedData);
            case "SERPENT-GCM":
                return decryptSerpentGcm(encryptedData);
            case "KUZNECHIK-CTR":
                return decryptKuznechikCtr(encryptedData);
            case "CHACHA20-POLY1305":
                return decryptChaCha20(encryptedData);
            case "TWOFISH-GCM":
                return decryptTwofishGcm(encryptedData);
            case "CAMELLIA-GCM":
                return decryptCamelliaGcm(encryptedData);
            case "MAGMA-CTR":
                return decryptMagmaCtr(encryptedData);
            default:
                throw new IllegalArgumentException("Неизвестный алгоритм: " + ALGORITHM);
        }
    }

    // AES-GCM
    private static byte[] decryptAesGcm(byte[] data) throws Exception {
        byte[] iv = Arrays.copyOfRange(data, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(data, 12, data.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "AES");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }

    // Serpent-GCM
    private static byte[] decryptSerpentGcm(byte[] data) throws Exception {
        byte[] iv = Arrays.copyOfRange(data, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(data, 12, data.length);
        Cipher cipher = Cipher.getInstance("Serpent/GCM/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "Serpent");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }

// Кузнечик-CTR
private static byte[] decryptKuznechikCtr(byte[] data) throws Exception {
    byte[] iv = Arrays.copyOfRange(data, 0, 8); // ✅ ИСПРАВЛЕНО: 8 байт
    byte[] encrypted = Arrays.copyOfRange(data, 8, data.length);
    Cipher cipher = Cipher.getInstance("GOST3412-2015/CTR/NoPadding", "BC");
    SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "GOST3412-2015");
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
    return cipher.doFinal(encrypted);
}

    // ChaCha20-Poly1305
    private static byte[] decryptChaCha20(byte[] data) throws Exception {
        byte[] iv = Arrays.copyOfRange(data, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(data, 12, data.length);
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "ChaCha20");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(encrypted);
    }

    // Twofish-GCM
    private static byte[] decryptTwofishGcm(byte[] data) throws Exception {
        byte[] iv = Arrays.copyOfRange(data, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(data, 12, data.length);
        Cipher cipher = Cipher.getInstance("Twofish/GCM/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "Twofish");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }

    // Camellia-GCM
    private static byte[] decryptCamelliaGcm(byte[] data) throws Exception {
        byte[] iv = Arrays.copyOfRange(data, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(data, 12, data.length);
        Cipher cipher = Cipher.getInstance("Camellia/GCM/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "Camellia");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }

    // Магма-CTR
    private static byte[] decryptMagmaCtr(byte[] data) throws Exception {
        byte[] iv = Arrays.copyOfRange(data, 0, 8); // У Магмы IV = 8 байт
        byte[] encrypted = Arrays.copyOfRange(data, 8, data.length);
        Cipher cipher = Cipher.getInstance("GOST28147/CTR/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "GOST28147");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(encrypted);
    }
}