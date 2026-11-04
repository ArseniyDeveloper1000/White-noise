package mainCode.diagnostic;

import mainCode.client.cryptoEncrypt;
import mainCode.client.imageCompress;
import mainCode.server.cryptoDecrypt;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.Frame;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

import com.sun.management.ThreadMXBean;

public class videoMemoryDiagnostic {

    private static final String[] ALGORITHMS = {
        "AES-GCM",
        "SERPENT-GCM",
        "KUZNECHIK-CTR",
        "CHACHA20-POLY1305",
        "TWOFISH-GCM",
        "CAMELLIA-GCM",
        "MAGMA-CTR"
    };

    private static final String[] VIDEO_FILES = {
        "test_320x240.mp4",
        "test_640x480.mp4",
        "test_960x720.mp4",
        "test_1280x720.mp4"
    };

    private static final int WARMUP_FRAMES = 10;

    public static void main(String[] args) {

        // Получаем средство измерения выделенной памяти потока
        java.lang.management.ThreadMXBean baseBean =
                ManagementFactory.getThreadMXBean();

        if (!(baseBean instanceof ThreadMXBean)) {
            System.out.println("Измерение выделенной памяти не поддерживается.");
            return;
        }

        ThreadMXBean memoryBean = (ThreadMXBean) baseBean;

        if (!memoryBean.isThreadAllocatedMemorySupported()) {
            System.out.println("JVM не поддерживает измерение выделенной памяти.");
            return;
        }

        memoryBean.setThreadAllocatedMemoryEnabled(true);

        long threadId = Thread.currentThread().getId();

        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }

        String csvPath = "results/video_memory_diagnostic.csv";

        try (PrintWriter csvWriter =
                     new PrintWriter(new FileWriter(csvPath))) {

            csvWriter.println(
                "Algorithm;VideoFile;Resolution;TotalFrames;" +
                "Avg_Encrypt_Allocated_MB;" +
                "Max_Encrypt_Allocated_MB;" +
                "Avg_Decrypt_Allocated_MB;" +
                "Max_Decrypt_Allocated_MB"
            );

            for (String algorithm : ALGORITHMS) {

                System.out.println("\n" + "=".repeat(70));
                System.out.println("Алгоритм: " + algorithm);

                for (String videoFile : VIDEO_FILES) {

                    runTest(
                        videoFile,
                        algorithm,
                        csvWriter,
                        memoryBean,
                        threadId
                    );
                }
            }

            System.out.println("\nДиагностика завершена.");
            System.out.println("Результаты: " +
                    new File(csvPath).getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runTest(
            String videoFile,
            String algorithm,
            PrintWriter csvWriter,
            ThreadMXBean memoryBean,
            long threadId
    ) throws Exception {

        File file = new File(videoFile);

        if (!file.exists()) {
            System.out.println("Файл не найден: " + videoFile);
            return;
        }

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file);
        Java2DFrameConverter converter = new Java2DFrameConverter();

        try {
            grabber.start();

            cryptoEncrypt encrypter = new cryptoEncrypt(algorithm);
            cryptoDecrypt decrypter = new cryptoDecrypt(algorithm);

            long checksum = 0;

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();

            System.out.printf(
                "\nВидео: %s | Разрешение: %dx%d%n",
                videoFile, width, height
            );

            // Прогрев алгоритмов
            for (int i = 0; i < WARMUP_FRAMES; i++) {

                Frame frame = grabber.grabImage();

                if (frame == null) {
                    break;
                }

                BufferedImage image = converter.convert(frame);

                // Сжатие не входит в замер
                byte[] compressed = imageCompress.compress(image);

                // Шифрование
                byte[] encrypted = encrypter.encrypt(compressed);

                // Дешифрование
                byte[] decrypted = decrypter.decrypt(encrypted);

                // Используем результат, чтобы он не оставался ненужным
                if (decrypted.length > 0) {
                    checksum += decrypted[0];
                }
            }

            grabber.setFrameNumber(0);

            long totalEncryptBytes = 0;
            long totalDecryptBytes = 0;

            long maxEncryptBytes = 0;
            long maxDecryptBytes = 0;

            int frameCount = 0;

            Frame frame;

            // Основной тест
            while ((frame = grabber.grabImage()) != null) {

                BufferedImage image = converter.convert(frame);

                // Сжатие выполняется вне измерения
                byte[] compressed = imageCompress.compress(image);

                // Измерение памяти при шифровании
                long beforeEncrypt =
                        memoryBean.getThreadAllocatedBytes(threadId);

                byte[] encrypted = encrypter.encrypt(compressed);

                long afterEncrypt =
                        memoryBean.getThreadAllocatedBytes(threadId);

                long encryptBytes = afterEncrypt - beforeEncrypt;

                // Измерение памяти при дешифровании
                long beforeDecrypt =
                        memoryBean.getThreadAllocatedBytes(threadId);

                byte[] decrypted = decrypter.decrypt(encrypted);

                long afterDecrypt =
                        memoryBean.getThreadAllocatedBytes(threadId);

                long decryptBytes = afterDecrypt - beforeDecrypt;

                // Суммарные значения
                totalEncryptBytes += encryptBytes;
                totalDecryptBytes += decryptBytes;

                // Максимальные значения для одного кадра
                maxEncryptBytes = Math.max(maxEncryptBytes, encryptBytes);
                maxDecryptBytes = Math.max(maxDecryptBytes, decryptBytes);

                frameCount++;

                if (decrypted.length > 0) {
                    checksum += decrypted[0];
                }
            }

            if (frameCount > 0) {

                double avgEncryptMB =
                        totalEncryptBytes / (double) frameCount /
                        (1024.0 * 1024.0);

                double avgDecryptMB =
                        totalDecryptBytes / (double) frameCount /
                        (1024.0 * 1024.0);

                double maxEncryptMB =
                        maxEncryptBytes / (1024.0 * 1024.0);

                double maxDecryptMB =
                        maxDecryptBytes / (1024.0 * 1024.0);

                System.out.printf(
                    "Кадров: %d | Шифрование: %.4f МБ/кадр | " +
                    "Дешифрование: %.4f МБ/кадр%n",
                    frameCount,
                    avgEncryptMB,
                    avgDecryptMB
                );

                System.out.printf(
                    "Максимум за кадр: шифрование %.4f МБ | " +
                    "дешифрование %.4f МБ%n",
                    maxEncryptMB,
                    maxDecryptMB
                );

                csvWriter.printf(
                    "%s;%s;%dx%d;%d;%.6f;%.6f;%.6f;%.6f%n",
                    algorithm,
                    videoFile,
                    width,
                    height,
                    frameCount,
                    avgEncryptMB,
                    maxEncryptMB,
                    avgDecryptMB,
                    maxDecryptMB
                );

                csvWriter.flush();
            }

        } finally {
            grabber.stop();
            grabber.release();
        }
    }
}