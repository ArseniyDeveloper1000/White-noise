package mainCode;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;



// В main():




public class diagNostic {
    static {
    Security.addProvider(new BouncyCastleProvider());
}
    public static void main(String args[]) {
        for (java.security.Provider provider : Security.getProviders()) {
    for (java.util.Map.Entry<Object, Object> entry : provider.entrySet()) {
        String key = entry.getKey().toString();
        if (key.contains("Cipher") && 
            (key.contains("GOST") || key.contains("Kuznechik") || key.contains("Magma"))) {
            System.out.println(key + " = " + entry.getValue());
        }
    }
}
        
    }
    
}
