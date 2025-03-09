import java.io.Serializable;

public class TokenInfo implements Serializable {
    String token;  // One-time use token issued by the server
    long expiryTime; // Expiration time as a Unix timestamp
}
