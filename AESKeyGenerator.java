import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.FileOutputStream;

public class AESKeyGenerator {
    public static void main(String[] args) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();
        byte[] encodedKey = secretKey.getEncoded();
        try (FileOutputStream keyFile = new FileOutputStream("keys/testKey.aes")) {
            keyFile.write(encodedKey);
        }

        System.out.println("AES Key generated and saved to testKey.aes");
    }
}
