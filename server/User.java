import java.net.Socket;

public class User {

    String username;
    String password;
    String publicKey;
    Socket socket;

    public User(String username, String password, String publicKey) {
        this.username = username;
        this.password = password;
        this.publicKey = publicKey;
    }
}