import java.util.concurrent.ConcurrentHashMap;

public class UserManager {

    static ConcurrentHashMap<String, User> users =
            new ConcurrentHashMap<>();

    public static boolean register(
            String username,
            String password,
            String publicKey
    ) {

        if (users.containsKey(username))
            return false;

        users.put(username,
                new User(username, password, publicKey));

        return true;
    }

    public static boolean login(
            String username,
            String password,
            java.net.Socket socket
    ) {

        User user = users.get(username);

        if (user != null && user.password.equals(password)) {

            user.socket = socket;
            return true;
        }

        return false;
    }

    public static User getUser(String username) {

        return users.get(username);
    }

    public static void logout(String username) {

        User user = users.get(username);

        if (user != null)
            user.socket = null;
    }
}