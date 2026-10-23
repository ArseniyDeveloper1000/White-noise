package mainCode.diagnostic;

import mainCode.client.cryptoEncrypt;
import mainCode.server.cryptoDecrypt;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.util.Locale;

public class delayDiagnostic {
    private static final String[] algorithms = {
        "AES-GCM", "SERPENT-GCM", "KUZNECHIK-CTR", 
        "CHACHA20-POLY1305", "TWOFISH-GCM", "CAMELLIA-GCM", "MAGMA-CTR"
    };
    
    // Массивы для удобного отображения на графиках
    private static final String[] resolutions = {
        "320x240", "480x360", "640x480", "960x720", "1280x960"
    };
    
    private static final int[] dataSizes = {
        320 * 240 * 3,   // ≈ 225 КБ
        480 * 360 * 3,   // ≈ 506 КБ
        640 * 480 * 3,   // ≈ 900 КБ
        960 * 720 * 3,   // ≈ 2025 КБ
        1280 * 960 * 3   // ≈ 3600 КБ
    };

    private static final String CSV_FILE_PATH = "results/diagnostic_results.csv";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static void getDelay(int dataSizeBytes, String resolution, int durationSec, int algorithm, PrintWriter csvWriter) throws Exception {
        String algo = algorithms[algorithm];
        
        System.out.printf("🧪 Тест: %-18s | Разрешение: %-8s | Размер: %-4d КБ | Длительность: %d сек%n",
            algo, resolution, dataSizeBytes / 1024, durationSec);

        cryptoEncrypt encrypter = new cryptoEncrypt(algo);
        cryptoDecrypt decrypter = new cryptoDecrypt(algo);
        
        byte[] testData = new byte[dataSizeBytes];
        RANDOM.nextBytes(testData);
        
        // 🔥 ПРОГРЕВ
        int warmupIterations = 50;
        long warmupChecksum = 0;
        for (int i = 0; i < warmupIterations; i++) {
            byte[] encrypted = encrypter.encrypt(testData);
            byte[] decrypted = decrypter.decrypt(encrypted);
            warmupChecksum += encrypted[0] + decrypted[0]; // Упрощенный checksum для скорости
        }

        // 📏 ИЗМЕРЕНИЕ
        int iterationCount = 0;
        long totalEncTimeNs = 0;
        long totalDecTimeNs = 0;
        
        long testStartNs = System.nanoTime();
        long testDurationNs = durationSec * 1_000_000_000L;
        
        while (System.nanoTime() - testStartNs < testDurationNs) {
            long encStart = System.nanoTime();
            byte[] encryptedBytes = encrypter.encrypt(testData);
            long encEnd = System.nanoTime();
            
            long decStart = System.nanoTime();
            byte[] decryptedBytes = decrypter.decrypt(encryptedBytes);
            long decEnd = System.nanoTime();
            
            // Защита от JIT
            totalEncTimeNs += (encEnd - encStart);
            totalDecTimeNs += (decEnd - decStart);
            iterationCount++;
        }
        
        if (iterationCount > 0) {
            // Переводим наносекунды в миллисекунды
            double avgEncMs = (totalEncTimeNs / (double) iterationCount) / 1_000_000.0;
            double avgDecMs = (totalDecTimeNs / (double) iterationCount) / 1_000_000.0;
            
            System.out.printf("   ✅ Итераций: %-5d | Шифр: %-6.4f мс | Дешифр: %-6.4f мс%n",
                iterationCount, avgEncMs, avgDecMs);
            
            // Запись в CSV (используем Locale.US, чтобы разделителем была точка, а не запятая)
            csvWriter.printf(Locale.US, "%s;%s;%d;%d;%.4f;%.4f%n",
                algo, resolution, dataSizeBytes, iterationCount, avgEncMs, avgDecMs);
            csvWriter.flush();
        }
    }
    
    public static void runFullDiagnostic() throws Exception {
        int durationSec = 10; 
        
        new File("results").mkdirs();
        System.out.println("🚀 Запуск расширенной диагностики...\n");
        
        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(CSV_FILE_PATH))) {
            // Новый формат: Algorithm;Resolution;DataSizeBytes;Iterations;Avg_Encrypt_Ms;Avg_Decrypt_Ms
            csvWriter.println("Algorithm;Resolution;DataSizeBytes;Iterations;Avg_Encrypt_Ms;Avg_Decrypt_Ms");
            
            for (int algoIdx = 0; algoIdx < algorithms.length; algoIdx++) {
                for (int i = 0; i < dataSizes.length; i++) {
                    getDelay(dataSizes[i], resolutions[i], durationSec, algoIdx, csvWriter);
                }
                System.out.println("-".repeat(70));
            }
        }
        System.out.println("✅ Диагностика завершена! Файл: " + new File(CSV_FILE_PATH).getAbsolutePath());
    }
    
    public static void main(String[] args) throws Exception {
        runFullDiagnostic();
    }
}