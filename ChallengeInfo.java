import java.io.Serializable;

public class ChallengeInfo implements Serializable {
    byte[] response; // Server's response to client's challenge (signature)
    String serverChallenge; // Server's challenge to the client
}
