package mainCode.diagnostic;

import mainCode.client.cryptoEncrypt;
import mainCode.server.imageDecompress;
import mainCode.server.cryptoDecrypt;
import mainCode.client.imageCompress;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;

public class videoCompressionDiagnostic {
    
    private static final String[] ALGORITHMS = {
        "AES-GCM", "SERPENT-GCM", "KUZNECHIK-CTR", 
        "CHACHA20-POLY1305", "TWOFISH-GCM", "CAMELLIA-GCM", "MAGMA-CTR"
    };
    
    private static final String SOURCE_VIDEO = "images/source.mp4"; 
    
    private static final String[] VIDEO_FILES = {
        "test_320x240.mp4", "test_640x480.mp4", "test_960x720.mp4", "test_1280x720.mp4"
    };
    
    private static final int[][] RESOLUTIONS = {
        {320, 240}, {640, 480}, {960, 720}, {1280, 720}
    };

    private static final int WARMUP_FRAMES = 10;

    public static void main(String[] args) {
        System.out.println("🚀 Шаг 1: Подготовка видеофайлов...");
        prepareVideos();
        
        System.out.println("\n Шаг 2: Диагностика пайплайна на реальном видео\n");
        File resultsDir = new File("results");
        if (!resultsDir.exists()) resultsDir.mkdirs();
        
        String csvPath = "results/video_pipeline_separated.csv";

        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(csvPath))) {
            csvWriter.println("Algorithm;VideoFile;Resolution;TotalFrames;OriginalSize_Bytes;Avg_CompressedSize_Bytes;Compression_Ratio_Percent;Avg_ClientTime_Ms;Avg_ServerTime_Ms");

            for (String algo : ALGORITHMS) {
                System.out.println("=".repeat(70));
                System.out.println("🔐 Алгоритм: " + algo);
                for (String videoFile : VIDEO_FILES) {
                    runTest(videoFile, algo, csvWriter);
                }
            }
            System.out.println("\n✅ Готово! Результаты в: " + new File(csvPath).getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void prepareVideos() {
        File source = new File(SOURCE_VIDEO);
        if (!source.exists()) {
            System.err.println("❌ Файл " + SOURCE_VIDEO + " не найден.");
            System.exit(1);
        }

        for (int i = 0; i < VIDEO_FILES.length; i++) {
            File target = new File(VIDEO_FILES[i]);
            if (!target.exists()) {
                System.out.println("   🔄 Генерация " + VIDEO_FILES[i] + " через FFmpeg...");
                int w = RESOLUTIONS[i][0];
                int h = RESOLUTIONS[i][1];
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg", "-i", SOURCE_VIDEO, "-vf", "scale=" + w + ":" + h, 
                        "-c:v", "libx264", "-an", "-y", VIDEO_FILES[i]
                    );
                    pb.inheritIO();
                    Process p = pb.start();
                    p.waitFor();
                } catch (Exception e) {
                    System.err.println("❌ Ошибка FFmpeg. Убедитесь, что ffmpeg установлен и добавлен в PATH.");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("   ✅ Видеофайлы готовы.");
    }

    private static void runTest(String videoFile, String algorithm, PrintWriter csvWriter) throws Exception {
        File file = new File(videoFile);
        if (!file.exists()) return;

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file);
        grabber.start();
        Java2DFrameConverter converter = new Java2DFrameConverter();
        
        cryptoEncrypt encrypter = new cryptoEncrypt(algorithm);
        cryptoDecrypt decrypter = new cryptoDecrypt(algorithm);

        int width = grabber.getImageWidth();
        int height = grabber.getImageHeight();
        int originalSizeBytes = width * height * 3;
        
        System.out.printf("   🎥 %-20s | %dx%d | Сырой кадр: %d КБ%n", 
            videoFile, width, height, originalSizeBytes / 1024);

        // ПРОГРЕВ
        long checksum = 0;
        for (int i = 0; i < WARMUP_FRAMES; i++) {
            Frame frame = grabber.grabImage();
            if (frame == null) break;
            BufferedImage img = converter.convert(frame);
            byte[] comp = imageCompress.compress(img);
            byte[] enc = encrypter.encrypt(comp);
            byte[] dec = decrypter.decrypt(enc);
            imageDecompress.decompress(dec);
            checksum += enc[0]; 
        }
        grabber.setFrameNumber(0);

        // ОСНОВНОЙ ТЕСТ
        int frameCount = 0;
        long totalClientTimeNs = 0;
        long totalServerTimeNs = 0;
        long totalCompressedSize = 0;

        Frame frame;
        while ((frame = grabber.grabImage()) != null) {
            BufferedImage img = converter.convert(frame);
            
            long startClient = System.nanoTime();
            byte[] compressed = imageCompress.compress(img);
            byte[] encrypted = encrypter.encrypt(compressed);
            long endClient = System.nanoTime();
            
            long startServer = System.nanoTime();
            byte[] decrypted = decrypter.decrypt(encrypted);
            BufferedImage resultImg = imageDecompress.decompress(decrypted);
            long endServer = System.nanoTime();
            
            totalCompressedSize += compressed.length;
            totalClientTimeNs += (endClient - startClient);
            totalServerTimeNs += (endServer - startServer);
            frameCount++;
            
            checksum += encrypted[0] + resultImg.getWidth();
        }
        grabber.stop();

        if (frameCount > 0) {
            double avgClientMs = (totalClientTimeNs / frameCount) / 1_000_000.0;
            double avgServerMs = (totalServerTimeNs / frameCount) / 1_000_000.0;
            double avgCompressedSize = (double) totalCompressedSize / frameCount;
            double compressionRatioPercent = (avgCompressedSize / originalSizeBytes) * 100.0;

            System.out.printf("      ✅ Кадров: %-4d | Сжатие: %5.1f%% | Клиент: %-6.3f мс | Сервер: %-6.3f мс%n",
                frameCount, compressionRatioPercent, avgClientMs, avgServerMs);

            csvWriter.printf("%s;%s;%dx%d;%d;%d;%.2f;%.4f;%.4f%n",
                algorithm, videoFile, width, height, frameCount, 
                originalSizeBytes, compressionRatioPercent, avgClientMs, avgServerMs);
            csvWriter.flush();
        }
    }
}
