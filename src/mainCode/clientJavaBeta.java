package mainCode;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.security.Security;
import java.security.SecureRandom;

import org.bytedeco.javacv.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class clientJavaBeta
{
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    

    // private static final String ALGORITHM = "AES-GCM";
    // private static final String ALGORITHM = "SERPENT-GCM";
    private static final String ALGORITHM = "KUZNECHIK-CBC";
    
    private static final byte[] AES_SECRET_KEY = "MySuperSecretKey1234567890123456".getBytes();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Java2DFrameConverter CONVERTER = new Java2DFrameConverter();
    
    public static void main(String[] args)
    {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.setImageWidth(480);
        grabber.setImageHeight(360);

        CanvasFrame originalView = new CanvasFrame("1. КЛИЕНТ: Исходное видео");
        CanvasFrame encryptedView = new CanvasFrame("2. КЛИЕНТ: Зашифровано");
        
        String serverAddress = "127.0.0.1";
        int port = 5002;

        System.out.println("🔐 Используемый алгоритм: " + ALGORITHM);

        try
        {
            grabber.start();
            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            
            DatagramSocket socket = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(serverAddress);

            Frame frame;
            int frameId = 0;
            long totalEncryptTimeNs = 0;
            
            while ((frame = grabber.grab()) != null)
            {
                frameId++;
                BufferedImage img = CONVERTER.convert(frame);
                originalView.showImage(img);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", baos);
                byte[] compressedBytes = baos.toByteArray();
                
                long encryptStart = System.nanoTime();
                byte[] encryptedBytes = encrypt(compressedBytes);
                long encryptEnd = System.nanoTime();
                totalEncryptTimeNs += (encryptEnd - encryptStart);
                
                BufferedImage noiseImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                byte[] noisePixels = ((DataBufferByte) noiseImg.getRaster().getDataBuffer()).getData();
                System.arraycopy(encryptedBytes, 0, noisePixels, 0, Math.min(encryptedBytes.length, noisePixels.length));
                encryptedView.showImage(noiseImg);
                
                long sendTime = System.nanoTime();
                ByteBuffer header = ByteBuffer.allocate(16);
                header.putInt(frameId);
                header.putInt(encryptedBytes.length);
                header.putLong(sendTime);
                
                byte[] packetData = new byte[16 + encryptedBytes.length];
                System.arraycopy(header.array(), 0, packetData, 0, 16);
                System.arraycopy(encryptedBytes, 0, packetData, 16, encryptedBytes.length);
                
                DatagramPacket udpPacket = new DatagramPacket(packetData, packetData.length, serverAddr, port);
                socket.send(udpPacket);
                
                if (frameId % 30 == 0)
                {
                    long avgEncryptTimeMs = (totalEncryptTimeNs / 30) / 1_000_000;
                    System.out.printf("[%s] Кадр %d | JPEG: %d б | Зашифровано: %d б | Время: %d мс%n", 
                        ALGORITHM, frameId, compressedBytes.length, encryptedBytes.length, avgEncryptTimeMs);
                    totalEncryptTimeNs = 0;
                }
                
                if (originalView.waitKey(10) != null) break;
            }
            
            socket.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try { grabber.stop(); } catch (Exception e) {}
            originalView.dispose();
            encryptedView.dispose();
        }
    }
    
    private static byte[] encrypt(byte[] data) throws Exception
    {
        switch (ALGORITHM) {
            case "AES-GCM":
                return encryptAesGcm(data);
            case "SERPENT-GCM":
                return encryptSerpentGcm(data);
            case "KUZNECHIK-CBC":
                return encryptKuznechikCbc(data);
            default:
                throw new IllegalArgumentException("Неизвестный алгоритм: " + ALGORITHM);
        }
    }
    
    // 1. AES-256-GCM
    private static byte[] encryptAesGcm(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec key = new SecretKeySpec(AES_SECRET_KEY, "AES");
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data);
        
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }
    
    // 2. SERPENT-256-GCM (через Bouncy Castle)
    private static byte[] encryptSerpentGcm(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("Serpent/GCM/NoPadding", "BC");
        SecretKeySpec key = new SecretKeySpec(AES_SECRET_KEY, "Serpent");
        byte[] iv = new byte[16];
        RANDOM.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data);
        
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }
    

    // 3. КУЗНЕЧИК (ГОСТ Р 34.12-2015) через Bouncy Castle
private static byte[] encryptKuznechikCbc(byte[] data) throws Exception {

    Cipher cipher = Cipher.getInstance("GOST3412-2015/CBC/PKCS7Padding", "BC");
    SecretKeySpec key = new SecretKeySpec(AES_SECRET_KEY, "GOST3412-2015");
    byte[] iv = new byte[16]; // Кузнечик имеет блок 16 байт
    RANDOM.nextBytes(iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
    byte[] encrypted = cipher.doFinal(data);
    
    byte[] result = new byte[iv.length + encrypted.length];
    System.arraycopy(iv, 0, result, 0, iv.length);
    System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
    return result;
}
}
/*
java --enable-native-access=ALL-UNNAMED -cp "lib\*;src\main\java" clientJavaBeta
javac -cp "lib\*" src\main\java\clientJavaBeta.java
*/
