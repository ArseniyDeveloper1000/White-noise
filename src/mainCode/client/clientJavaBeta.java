// package mainCode.client;


// import java.net.DatagramPacket;
// import java.net.DatagramSocket;
// import java.net.InetAddress;
// import java.nio.ByteBuffer;




// import org.bytedeco.javacv.*;





// import java.awt.image.BufferedImage;
// import java.awt.image.DataBufferByte;

// public class clientJavaBeta
// {

    

//     // private static final String ALGORITHM = "AES-GCM";
//     // private static final String ALGORITHM = "SERPENT-GCM";
//     private static final String ALGORITHM = "KUZNECHIK-CBC";
    

//     private static final Java2DFrameConverter CONVERTER = new Java2DFrameConverter();
    
//     public static void main(String[] args)
//     {
//         OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
//         grabber.setImageWidth(480);
//         grabber.setImageHeight(360);

//         CanvasFrame originalView = new CanvasFrame("1. КЛИЕНТ: Исходное видео");
//         CanvasFrame encryptedView = new CanvasFrame("2. КЛИЕНТ: Зашифровано");
        
//         String serverAddress = "127.0.0.1";
//         int port = 5002;

//         System.out.println("🔐 Используемый алгоритм: " + ALGORITHM);

//         try
//         {
//             grabber.start();
//             int width = grabber.getImageWidth();
//             int height = grabber.getImageHeight();
            
//             DatagramSocket socket = new DatagramSocket();
//             InetAddress serverAddr = InetAddress.getByName(serverAddress);

//             Frame frame;
//             int frameId = 0;
//             long totalEncryptTimeNs = 0;

//             cryptoEncrypt encrypter = new cryptoEncrypt();
            
//             while ((frame = grabber.grab()) != null)
//             {
//                 frameId++;
//                 BufferedImage img = CONVERTER.convert(frame);
//                 originalView.showImage(img);
                

//                 byte[] compressedBytes = imageCompress.compress(img);
                
//                 long encryptStart = System.currentTimeMillis();
//                 byte[] encryptedBytes = encrypter.encrypt(compressedBytes);
//                 long encryptEnd = System.currentTimeMillis();
//                 totalEncryptTimeNs += (encryptEnd - encryptStart);
                
//                 BufferedImage noiseImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
//                 byte[] noisePixels = ((DataBufferByte) noiseImg.getRaster().getDataBuffer()).getData();
//                 System.arraycopy(encryptedBytes, 0, noisePixels, 0, Math.min(encryptedBytes.length, noisePixels.length));
//                 encryptedView.showImage(noiseImg);
                
//                 long sendTime = System.currentTimeMillis();
//                 ByteBuffer header = ByteBuffer.allocate(16);
//                 header.putInt(frameId);
//                 header.putInt(encryptedBytes.length);
//                 header.putLong(sendTime);
                
//                 byte[] packetData = new byte[16 + encryptedBytes.length];
//                 System.arraycopy(header.array(), 0, packetData, 0, 16);
//                 System.arraycopy(encryptedBytes, 0, packetData, 16, encryptedBytes.length);
                
//                 DatagramPacket udpPacket = new DatagramPacket(packetData, packetData.length, serverAddr, port);
//                 socket.send(udpPacket);
                
//                 if (frameId % 30 == 0)
//                 {
//                     long avgEncryptTimeMs = (totalEncryptTimeNs / 30) / 1_000_000;
//                     System.out.printf("[%s] Кадр %d | JPEG: %d б | Зашифровано: %d б | Время: %d мс%n", 
//                         ALGORITHM, frameId, compressedBytes.length, encryptedBytes.length, avgEncryptTimeMs);
//                     totalEncryptTimeNs = 0;
//                 }
                
//                 if (originalView.waitKey(10) != null) break;
//             }
            
//             socket.close();
//         }
//         catch (Exception e) {
//             e.printStackTrace();
//         }
//         finally {
//             try { grabber.stop(); } catch (Exception e) {}
//             originalView.dispose();
//             encryptedView.dispose();
//         }
//     }
// }
/*
java --enable-native-access=ALL-UNNAMED -cp "lib\*;src\main\java" clientJavaBeta
javac -cp "lib\*" src\main\java\clientJavaBeta.java
*/

package mainCode.client;


import org.bytedeco.javacv.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class clientJavaBeta {
    private static final String ALGORITHM = "AES-GCM";
    private static final Java2DFrameConverter CONVERTER = new Java2DFrameConverter();

    public static void main(String[] args) {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.setImageWidth(480);
        grabber.setImageHeight(360);

        CanvasFrame originalView = new CanvasFrame("1. КЛИЕНТ: Исходное видео");
        CanvasFrame encryptedView = new CanvasFrame("2. КЛИЕНТ: Зашифровано");
        
        String serverAddress = "127.0.0.1"; // Или "192.168.1.2"
        int port = 5002;

        System.out.println("🔐 Используемый алгоритм: " + ALGORITHM);

        try {
            grabber.start();
            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            
            DatagramSocket socket = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(serverAddress);

            cryptoEncrypt encrypter = new cryptoEncrypt();
            Frame frame;
            int frameId = 0;
            
            // Переменные для измерения общей задержки шифрования
            long totalEncryptTimeNs = 0;
            long testStartNs = System.nanoTime();

            while ((frame = grabber.grab()) != null) {
                // Ограничение времени теста в 30 секунд (как в Варианте А)
                if (System.nanoTime() - testStartNs > 30_000_000_000L) break;

                 // 3. Формирование пакета
                // ИСПРАВЛЕНО: используем currentTimeMillis для корректного расчета задержки на сервере
                long captureTime = System.currentTimeMillis(); 
                frameId++;
                BufferedImage img = CONVERTER.convert(frame);
                originalView.showImage(img);

                // 1. Сжатие
                byte[] compressedBytes = imageCompress.compress(img);
                
                // 2. Шифрование
                long encryptStart = System.nanoTime();
                byte[] encryptedBytes = encrypter.encrypt(compressedBytes);
                long encryptEnd = System.nanoTime();
                totalEncryptTimeNs += (encryptEnd - encryptStart);

                // Визуализация шума
                BufferedImage noiseImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                byte[] noisePixels = ((DataBufferByte) noiseImg.getRaster().getDataBuffer()).getData();
                System.arraycopy(encryptedBytes, 0, noisePixels, 0, Math.min(encryptedBytes.length, noisePixels.length));
                encryptedView.showImage(noiseImg);

        
                
                ByteBuffer header = ByteBuffer.allocate(16);
                header.putInt(frameId);
                header.putInt(encryptedBytes.length);
                header.putLong(captureTime); // Передаем время захвата кадра
                
                byte[] packetData = new byte[16 + encryptedBytes.length];
                System.arraycopy(header.array(), 0, packetData, 0, 16);
                System.arraycopy(encryptedBytes, 0, packetData, 16, encryptedBytes.length);
                
                DatagramPacket udpPacket = new DatagramPacket(packetData, packetData.length, serverAddr, port);
                socket.send(udpPacket);

                // Статистика каждые 30 кадров
                if (frameId % 30 == 0) {
                    long avgEncryptTimeMs = (totalEncryptTimeNs / 30) / 1_000_000;
                    System.out.printf("[%s] Кадр %d | JPEG: %d б | Зашифровано: %d б | Ср. время шифр.: %d мс%n", 
                        ALGORITHM, frameId, compressedBytes.length, encryptedBytes.length, avgEncryptTimeMs);
                    totalEncryptTimeNs = 0; // Сброс для следующего интервала
                }
                
                if (originalView.waitKey(10) != null) break;
            }
            
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { grabber.stop(); } catch (Exception e) {}
            originalView.dispose();
            encryptedView.dispose();
        }
    }
}
