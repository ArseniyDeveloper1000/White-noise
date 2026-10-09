package mainCode;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.security.Security;
import java.util.Arrays;

import org.bytedeco.javacv.CanvasFrame;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class serverJavaBeta
{
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    

    // private static final String ALGORITHM = "AES-GCM";
    // private static final String ALGORITHM = "SERPENT-GCM";
    private static final String ALGORITHM = "KUZNECHIK-CBC";
    
    private static final byte[] AES_SECRET_KEY = "MySuperSecretKey1234567890123456".getBytes();
    
    public static void main(String[] args) throws Exception
    {
        int port = 5002;
        DatagramSocket socket = new DatagramSocket(port);
        System.out.println("UDP-сервер запущен на порту " + port);
        System.out.println("Алгоритм: " + ALGORITHM);

        CanvasFrame serverView = new CanvasFrame("СЕРВЕР: Расшифрованное видео");
        byte[] buffer = new byte[65535]; 
        
        int receivedCount = 0;
        int lostCount = 0;
        int expectedFrameId = 0;

        while (true)
        {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            
            receivedCount++;
            
            ByteBuffer data = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
            int frameId = data.getInt();
            int length = data.getInt();
            long clientTime = data.getLong();
            
            if (frameId != expectedFrameId && expectedFrameId != 0) {
                int lost = frameId - expectedFrameId;
                if (lost > 0) lostCount += lost;
            }
            expectedFrameId = frameId + 1;
            
            byte[] encryptedBytes = new byte[length];
            System.arraycopy(packet.getData(), 16, encryptedBytes, 0, length);
            
            byte[] compressedBytes;
            try {
                compressedBytes = decrypt(encryptedBytes);
            } catch (Exception e) {
                System.err.println("Ошибка дешифровки кадра " + frameId + ": " + e.getMessage());
                continue;
            }
            
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
                BufferedImage img = ImageIO.read(bais);
                if (img != null) serverView.showImage(img);
            } catch (Exception e) {
                System.err.println("Ошибка декодирования JPEG");
            }
            
            if (receivedCount % 30 == 0) {
                long delay = System.currentTimeMillis() - (clientTime / 1_000_000);
                System.out.printf("[%s] Получено: %d | Потерь: %d | Задержка: %d мс%n", 
                    ALGORITHM, receivedCount, lostCount, delay);
            }
            
            if (serverView.waitKey(5) != null) break;
        }
        
        socket.close();
        serverView.dispose();
    }
    
    private static byte[] decrypt(byte[] encryptedData) throws Exception
    {
        switch (ALGORITHM) {
            case "AES-GCM":
                return decryptAesGcm(encryptedData);
            case "SERPENT-GCM":
                return decryptSerpentGcm(encryptedData);
            case "KUZNECHIK-CBC":
                return decryptKuznechikCbc(encryptedData);
            default:
                throw new IllegalArgumentException("Неизвестный алгоритм");
        }
    }
    
    private static byte[] decryptAesGcm(byte[] data) throws Exception {
        byte[] iv = Arrays.copyOfRange(data, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(data, 12, data.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec key = new SecretKeySpec(AES_SECRET_KEY, "AES");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }
    
    private static byte[] decryptSerpentGcm(byte[] data) throws Exception {
        byte[] iv = Arrays.copyOfRange(data, 0, 16);
        byte[] encrypted = Arrays.copyOfRange(data, 16, data.length);
        Cipher cipher = Cipher.getInstance("Serpent/GCM/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(AES_SECRET_KEY, "Serpent");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }
    
    private static byte[] decryptKuznechikCbc(byte[] data) throws Exception {
    byte[] iv = Arrays.copyOfRange(data, 0, 16);
    byte[] encrypted = Arrays.copyOfRange(data, 16, data.length);
    Cipher cipher = Cipher.getInstance("GOST3412-2015/CBC/PKCS7Padding", "BC");
    SecretKeySpec key = new SecretKeySpec(AES_SECRET_KEY, "GOST3412-2015");
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
    return cipher.doFinal(encrypted);
}
}
/*
java --enable-native-access=ALL-UNNAMED -cp "lib\*;src\main\java" serverJavaBeta
javac -cp "lib\*" src\main\java\serverJavaBeta.java
*/