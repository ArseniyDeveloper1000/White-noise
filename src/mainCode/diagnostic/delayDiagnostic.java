package mainCode.diagnostic;

import mainCode.client.cryptoEncrypt;
import mainCode.server.cryptoDecrypt;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.security.SecureRandom;

public class delayDiagnostic {
    private static final String[] algorithms = {
    "AES-GCM", 
    "SERPENT-GCM", 
    "KUZNECHIK-CTR", 
    "CHACHA20-POLY1305", 
    "TWOFISH-GCM", 
    "CAMELLIA-GCM", 
    "MAGMA-CTR"
};
    private static final String CSV_FILE_PATH = "results/diagnostic_results.csv";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Чистый тест алгоритма на синтетических данных
     * @param dataSizeBytes Размер данных в байтах (имитация кадра)
     * @param durationSec Длительность теста
     * @param algorithm Индекс алгоритма
     */
    public static void getDelay(int dataSizeBytes, int durationSec, int algorithm, PrintWriter csvWriter) throws Exception {
        String algo = algorithms[algorithm];
        
        System.out.printf("🧪 Тест: %-12s | Размер данных: %d КБ | Длительность: %d сек%n",
            algo, dataSizeBytes / 1024, durationSec);

        cryptoEncrypt encrypter = new cryptoEncrypt(algo);
        cryptoDecrypt decrypter = new cryptoDecrypt(algo);
        
        // Генерируем случайные данные (имитация кадра)
        byte[] testData = new byte[dataSizeBytes];
        RANDOM.nextBytes(testData);
        
        // 🔥 ПРОГРЕВ (500 итераций — больше, т.к. нет задержки камеры)
        int warmupIterations = 500;
        System.out.printf("    Прогрев: %d итераций...%n", warmupIterations);
        long warmupChecksum = 0;
        
        for (int i = 0; i < warmupIterations; i++) {
            byte[] encrypted = encrypter.encrypt(testData);
            byte[] decrypted = decrypter.decrypt(encrypted);
            for (byte b : encrypted) warmupChecksum += b;
            for (byte b : decrypted) warmupChecksum += b;
        }
        System.out.println("   ✅ Прогрев завершен. Checksum: " + warmupChecksum);

        // 📏 ИЗМЕРЕНИЕ
        int iterationCount = 0;
        long totalCryptoTimeNs = 0;
        long minCryptoTimeNs = Long.MAX_VALUE;
        long maxCryptoTimeNs = Long.MIN_VALUE;
        long checksum = 0;
        
        long testStartNs = System.nanoTime();
        long testDurationNs = durationSec * 1_000_000_000L;
        
        while (System.nanoTime() - testStartNs < testDurationNs) {
            // ⏱️ ЗАМЕР
            long cryptoStartNs = System.nanoTime();
            
            byte[] encryptedBytes = encrypter.encrypt(testData);
            byte[] decryptedBytes = decrypter.decrypt(encryptedBytes);
            
            long cryptoEndNs = System.nanoTime();
            long cryptoTimeNs = cryptoEndNs - cryptoStartNs;
            
            // Защита от JIT
            for (byte b : encryptedBytes) checksum += b;
            for (byte b : decryptedBytes) checksum += b;
            
            iterationCount++;
            totalCryptoTimeNs += cryptoTimeNs;
            minCryptoTimeNs = Math.min(minCryptoTimeNs, cryptoTimeNs);
            maxCryptoTimeNs = Math.max(maxCryptoTimeNs, cryptoTimeNs);
        }
        
        if (iterationCount > 0) {
            double avgCryptoMs = (totalCryptoTimeNs / iterationCount) / 1_000_000.0;
            double minCryptoMs = minCryptoTimeNs / 1_000_000.0;
            double maxCryptoMs = maxCryptoTimeNs / 1_000_000.0;
            double iterationsPerSec = iterationCount / (double) durationSec;
            
            System.out.printf("   ✅ Итераций: %-5d | Общее время: %-7.4f мс | Мин: %-7.4f мс | Макс: %-7.4f мс | Итер/сек: %.2f%n",
                iterationCount, avgCryptoMs, minCryptoMs, maxCryptoMs, iterationsPerSec);
            
            csvWriter.printf("%s;%d;%d;%.4f;%.4f;%.4f;%.2f%n",
                algo, dataSizeBytes, iterationCount, 
                avgCryptoMs, minCryptoMs, maxCryptoMs, 
                iterationsPerSec);
            csvWriter.flush();
        }
    }
    
    public static void runFullDiagnostic() throws Exception {
        // Размеры данных, соответствующие реальным кадрам (width * height * 3 байта)
        // 320x240, 480x360, 640x480, 960x720, 1280x960
        int[] dataSizes = {
            320 * 240 * 3,     // 230 400 байт ≈ 225 КБ
            480 * 360 * 3,     // 518 400 байт ≈ 506 КБ
            640 * 480 * 3,     // 921 600 байт ≈ 900 КБ
            960 * 720 * 3,     // 2 073 600 байт ≈ 2025 КБ
            1280 * 960 * 3     // 3 686 400 байт ≈ 3600 КБ
        };
        
        int durationSec = 10; // 10 секунд на каждый тест (для синтетики достаточно)
        
        new File("results").mkdirs();
        System.out.println(" Запуск диагностики на синтетических данных...\n");
        
        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(CSV_FILE_PATH))) {
            csvWriter.println("Algorithm;DataSizeBytes;IterationCount;Avg_Crypto_Ms;Min_Crypto_Ms;Max_Crypto_Ms;IterationsPerSec");
            
            for (int algoIdx = 0; algoIdx < algorithms.length; algoIdx++) {
                for (int dataSize : dataSizes) {
                    getDelay(dataSize, durationSec, algoIdx, csvWriter);
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