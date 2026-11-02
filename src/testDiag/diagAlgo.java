package testDiag;



import java.security.Security;
import javax.crypto.Cipher;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class diagAlgo {
    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // Список алгоритмов для проверки
    private static final String[][] ALGORITHMS_TO_CHECK = {
        {"AES-GCM", "AES/GCM/NoPadding", "BC"},
        {"Serpent-GCM", "Serpent/GCM/NoPadding", "BC"},
        {"Kuznechik-CBC (GOST3412-2015)", "GOST3412-2015/CBC/PKCS7Padding", "BC"},
        {"ChaCha20-Poly1305", "ChaCha20-Poly1305", "BC"},
        {"Twofish-GCM", "Twofish/GCM/NoPadding", "BC"},
        {"Camellia-GCM", "Camellia/GCM/NoPadding", "BC"},
        {"Magma-CBC (GOST28147)", "GOST28147/CBC/PKCS7Padding", "BC"},
        
        // Дополнительные алгоритмы для проверки
        {"AES-CBC", "AES/CBC/PKCS7Padding", "BC"},
        {"Blowfish-CBC", "Blowfish/CBC/PKCS7Padding", "BC"},
        {"IDEA-CBC", "IDEA/CBC/PKCS7Padding", "BC"},
        {"RC6-CBC", "RC6/CBC/PKCS7Padding", "BC"},
        {"SEED-CBC", "SEED/CBC/PKCS7Padding", "BC"},
        {"ARIA-GCM", "ARIA/GCM/NoPadding", "BC"}
    };

    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("🔍 Диагностика доступности алгоритмов шифрования");
        System.out.println("=".repeat(70));
        
        // Проверка провайдера Bouncy Castle
        checkBouncyCastleProvider();
        
        System.out.println("\n📋 Проверка алгоритмов:");
        System.out.println("-".repeat(70));
        
        int availableCount = 0;
        int unavailableCount = 0;
        
        for (String[] algoInfo : ALGORITHMS_TO_CHECK) {
            String displayName = algoInfo[0];
            String transformation = algoInfo[1];
            String provider = algoInfo[2];
            
            boolean isAvailable = checkAlgorithm(transformation, provider);
            
            if (isAvailable) {
                System.out.printf("✅ %-35s | %s%n", displayName, transformation);
                availableCount++;
            } else {
                System.out.printf("❌ %-35s | %s%n", displayName, transformation);
                unavailableCount++;
            }
        }
        
        System.out.println("-".repeat(70));
        System.out.printf("📊 Итого: доступно %d, недоступно %d из %d алгоритмов%n", 
            availableCount, unavailableCount, ALGORITHMS_TO_CHECK.length);
        System.out.println("=".repeat(70));
    }
    
    /**
     * Проверяет информацию о провайдере Bouncy Castle
     */
    private static void checkBouncyCastleProvider() {
        System.out.println("\n🔐 Информация о провайдере:");
        
        // Проверяем, зарегистрирован ли Bouncy Castle
        if (Security.getProvider("BC") != null) {
            System.out.println("✅ Bouncy Castle провайдер зарегистрирован");
            System.out.printf("   Версия: %s%n", Security.getProvider("BC").getVersion());
            System.out.printf("   Информация: %s%n", Security.getProvider("BC").getInfo());
        } else {
            System.out.println("❌ Bouncy Castle провайдер НЕ зарегистрирован!");
            System.out.println("   Убедитесь, что библиотека bcprov-jdk15on.jar добавлена в classpath");
        }
    }
    
    /**
     * Проверяет доступность конкретного алгоритма
     * @param transformation Полное имя трансформации (например, "AES/GCM/NoPadding")
     * @param provider Имя провайдера ("BC" для Bouncy Castle)
     * @return true если алгоритм доступен, false иначе
     */
    private static boolean checkAlgorithm(String transformation, String provider) {
        try {
            // Пытаемся создать экземпляр Cipher
            Cipher cipher = Cipher.getInstance(transformation, provider);
            
            // Если успешно создали - алгоритм доступен
            return true;
            
        } catch (Exception e) {
            // Если возникла ошибка - алгоритм недоступен
            return false;
        }
    }
    
    /**
     * Дополнительная проверка: вывод всех доступных алгоритмов от Bouncy Castle
     */
    public static void listAllBouncyCastleAlgorithms() {
        System.out.println("\n📚 Полный список алгоритмов Bouncy Castle:");
        System.out.println("-".repeat(70));
        
        java.security.Provider bcProvider = Security.getProvider("BC");
        if (bcProvider != null) {
            // Выводим все сервисы провайдера
            bcProvider.getServices().stream()
                .filter(service -> service.getType().equals("Cipher"))
                .map(service -> service.getAlgorithm())
                .distinct()
                .sorted()
                .forEach(algo -> System.out.println("   • " + algo));
        } else {
            System.out.println("❌ Провайдер BC не найден");
        }
    }
}