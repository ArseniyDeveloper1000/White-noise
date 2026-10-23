package mainCode.client;

import java.security.SecureRandom;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class cryptoEncrypt {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private String ALGORITHM;
    private static final byte[] SECRET_KEY = "MySuperSecretKey1234567890123456".getBytes();
    private static final SecureRandom RANDOM = new SecureRandom();

    public cryptoEncrypt() {
        this.ALGORITHM = "AES-GCM";
    }

    public cryptoEncrypt(String algo) {
        this.ALGORITHM = algo;
    }

    
    public byte[] encrypt(byte[] data) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException(
                "Нельзя зашифровать данные: массив равен null"
            );
        }

        if (data.length == 0) {
            throw new IllegalArgumentException(
                "Нельзя зашифровать данные: массив пуст"
            );
        }

        switch (ALGORITHM) {
            case "AES-GCM":
                return encryptAesGcm(data);
            case "SERPENT-GCM":
                return encryptSerpentGcm(data);
            case "KUZNECHIK-CTR":
                return encryptKuznechikCtr(data);
            case "CHACHA20-POLY1305":
                return encryptChaCha20(data);
            case "TWOFISH-GCM":
                return encryptTwofishGcm(data);
            case "CAMELLIA-GCM":
                return encryptCamelliaGcm(data);
            case "MAGMA-CTR":
                return encryptMagmaCtr(data);
            default:
                throw new IllegalArgumentException(
                    "Неизвестный алгоритм: " + ALGORITHM
                );
        }
    }


    private static byte[] combineIvAndData(byte[] iv, byte[] encrypted) {
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }

    // AES-GCM
    private static byte[] encryptAesGcm(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "AES");
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data);
        return combineIvAndData(iv, encrypted);
    }

    // Serpent-GCM
    private static byte[] encryptSerpentGcm(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("Serpent/GCM/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "Serpent");
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data);
        return combineIvAndData(iv, encrypted);
    }

// Кузнечик-CTR (режим гаммирования)
private static byte[] encryptKuznechikCtr(byte[] data) throws Exception {
    Cipher cipher = Cipher.getInstance("GOST3412-2015/CTR/NoPadding", "BC");
    SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "GOST3412-2015");
    byte[] iv = new byte[8]; // ✅ ИСПРАВЛЕНО: IV для CTR = 8 байт (не 16!)
    RANDOM.nextBytes(iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
    byte[] encrypted = cipher.doFinal(data);
    return combineIvAndData(iv, encrypted);
}

    // ChaCha20-Poly1305
    private static byte[] encryptChaCha20(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "ChaCha20");
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);
        return combineIvAndData(iv, encrypted);
    }

    // Twofish-GCM
    private static byte[] encryptTwofishGcm(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("Twofish/GCM/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "Twofish");
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data);
        return combineIvAndData(iv, encrypted);
    }

    // Camellia-GCM
    private static byte[] encryptCamelliaGcm(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("Camellia/GCM/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "Camellia");
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data);
        return combineIvAndData(iv, encrypted);
    }

    // Магма-CTR (режим гаммирования)
    private static byte[] encryptMagmaCtr(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("GOST28147/CTR/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY, "GOST28147");
        byte[] iv = new byte[8]; // У Магмы IV = 8 байт
        RANDOM.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(data);
        return combineIvAndData(iv, encrypted);
    }
}