// package mainCode.server;
// import java.net.DatagramPacket;
// import java.net.DatagramSocket;
// import java.nio.ByteBuffer;
// import java.io.ByteArrayInputStream;
// import javax.imageio.ImageIO;
// import java.awt.image.BufferedImage;
// import java.security.Security;


// import org.bytedeco.javacv.CanvasFrame;
// import org.bouncycastle.jce.provider.BouncyCastleProvider;


// public class serverJavaBeta
// {
//     static {
//         Security.addProvider(new BouncyCastleProvider());
//     }
    

//     // private static final String ALGORITHM = "AES-GCM";
//     // private static final String ALGORITHM = "SERPENT-GCM";
//     private static final String ALGORITHM = "KUZNECHIK-CBC";
    
//     private static final byte[] AES_SECRET_KEY = "MySuperSecretKey1234567890123456".getBytes();
    
//     public static void main(String[] args) throws Exception
//     {
//         int port = 5002;
//         DatagramSocket socket = new DatagramSocket(port);
//         System.out.println("UDP-сервер запущен на порту " + port);
//         System.out.println("Алгоритм: " + ALGORITHM);

//         CanvasFrame serverView = new CanvasFrame("СЕРВЕР: Расшифрованное видео");
//         byte[] buffer = new byte[65535]; 
        
//         int receivedCount = 0;
//         int lostCount = 0;
//         int expectedFrameId = 0;

//         cryptoDecrypt decrypter = new cryptoDecrypt();


//         while (true)
//         {
//             DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//             socket.receive(packet);
            
//             receivedCount++;
            
//             ByteBuffer data = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
//             int frameId = data.getInt();
//             int length = data.getInt();
//             long clientTime = data.getLong();
            
//             if (frameId != expectedFrameId && expectedFrameId != 0) {
//                 int lost = frameId - expectedFrameId;
//                 if (lost > 0) lostCount += lost;
//             }
//             expectedFrameId = frameId + 1;
            
//             byte[] encryptedBytes = new byte[length];
//             System.arraycopy(packet.getData(), 16, encryptedBytes, 0, length);
            
//             byte[] compressedBytes;
//             try {
//                 compressedBytes = decrypter.decrypt(encryptedBytes);
//             } catch (Exception e) {
//                 System.err.println("Ошибка дешифровки кадра " + frameId + ": " + e.getMessage());
//                 continue;
//             }
            
//             try {
//                 BufferedImage img = imageDecompress.decompress(compressedBytes);
//                 if (img != null) serverView.showImage(img);
//             } catch (Exception e) {
//                 System.err.println("Ошибка декодирования JPEG");
//             }
            
//             if (receivedCount % 30 == 0) {
//                 long delay = System.currentTimeMillis() - (clientTime / 1_000_000);
//                 System.out.printf("[%s] Получено: %d | Потерь: %d | Задержка: %d мс%n", 
//                     ALGORITHM, receivedCount, lostCount, delay);
//             }
            
//             if (serverView.waitKey(5) != null) break;
//         }
        
//         socket.close();
//         serverView.dispose();
//     }
    
// }
/*
java --enable-native-access=ALL-UNNAMED -cp "lib\*;src\main\java" serverJavaBeta
javac -cp "lib\*" src\main\java\serverJavaBeta.java
*/
package mainCode.server;

import mainCode.server.cryptoDecrypt;
import mainCode.server.imageDecompress;

import org.bytedeco.javacv.CanvasFrame;
import java.awt.image.BufferedImage;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

public class serverJavaBeta {
    private static final String ALGORITHM = "AES-GCM";

    public static void main(String[] args) throws Exception {
        int port = 5002;
        DatagramSocket socket = new DatagramSocket(port);
        System.out.println("UDP-сервер запущен на порту " + port);
        System.out.println("Алгоритм: " + ALGORITHM);

        CanvasFrame serverView = new CanvasFrame("СЕРВЕР: Расшифрованное видео");
        byte[] buffer = new byte[65535]; 
        
        int receivedCount = 0;
        int lostCount = 0;
        int expectedFrameId = 0;

        cryptoDecrypt decrypter = new cryptoDecrypt();
        
        // Переменные для измерения ОБЩЕЙ задержки (End-to-End)
        long totalLatencyMs = 0;

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            
            receivedCount++;
            
            ByteBuffer data = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
            int frameId = data.getInt();
            int length = data.getInt();
            long clientCaptureTime = data.getLong(); // Получаем время захвата с клиента
            
            // Подсчет потерь
            if (frameId != expectedFrameId && expectedFrameId != 0) {
                int lost = frameId - expectedFrameId;
                if (lost > 0) lostCount += lost;
            }
            expectedFrameId = frameId + 1;
            
            byte[] encryptedBytes = new byte[length];
            System.arraycopy(packet.getData(), 16, encryptedBytes, 0, length);
            
            byte[] compressedBytes;
            try {
                compressedBytes = decrypter.decrypt(encryptedBytes);
            } catch (Exception e) {
                System.err.println("Ошибка дешифровки кадра " + frameId + ": " + e.getMessage());
                continue;
            }
            
            try {
                BufferedImage img = imageDecompress.decompress(compressedBytes);
                if (img != null) serverView.showImage(img);
                
                // ИСПРАВЛЕНО: Расчет истинной общей задержки (от захвата до отображения)
                long serverDisplayTime = System.currentTimeMillis();
                long currentLatency = serverDisplayTime - clientCaptureTime;
                totalLatencyMs += currentLatency;
                
            } catch (Exception e) {
                System.err.println("Ошибка декодирования JPEG кадра " + frameId);
            }
            
            // Статистика каждые 30 кадров
            if (receivedCount % 30 == 0) {
                double avgLatency = (double) totalLatencyMs / 30;
                System.out.printf("[%s] Получено: %d | Потерь: %d | СРЕДНЯЯ ОБЩАЯ ЗАДЕРЖКА: %.2f мс%n", 
                    ALGORITHM, receivedCount, lostCount, avgLatency);
                totalLatencyMs = 0; // Сброс для следующего интервала
            }
            
            if (serverView.waitKey(5) != null) break;
        }
        
        socket.close();
        serverView.dispose();
    }
}