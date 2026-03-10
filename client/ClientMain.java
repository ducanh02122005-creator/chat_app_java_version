import java.net.Socket;
import java.security.*;
import java.util.Scanner;

import org.json.JSONObject;

public class ClientMain {

    private static PrivateKey privateKey;
    private static String username;

    public static void main(String[] args) throws Exception {

        Socket socket = new Socket("127.0.0.1", 12345);
        Scanner sc = new Scanner(System.in);

        System.out.println("Connected to server.");

        // Start receiver thread
        startReceiver(socket);

        while (true) {

            System.out.println("\n1 Register");
            System.out.println("2 Login");
            System.out.println("3 Send message");
            System.out.println("4 Exit");

            System.out.print("> ");
            String choice = sc.nextLine();

            switch (choice) {

                // ======================
                // REGISTER
                // ======================
                case "1":

                    System.out.print("Username: ");
                    username = sc.nextLine();

                    System.out.print("Password: ");
                    String password = sc.nextLine();

                    KeyPair pair = CryptoUtils.generateRSAKeys();

                    privateKey = pair.getPrivate();

                    CryptoUtils.savePrivateKey(username, privateKey);

                    JSONObject reg = new JSONObject();

                    reg.put("type", "register");
                    reg.put("username", username);
                    reg.put("password", password);
                    reg.put(
                            "public_key",
                            CryptoUtils.serializePublicKey(pair.getPublic())
                    );

                    JsonUtil.sendJson(socket, reg);

                    break;

                // ======================
                // LOGIN
                // ======================
                case "2":

                    System.out.print("Username: ");
                    username = sc.nextLine();

                    System.out.print("Password: ");
                    password = sc.nextLine();

                    privateKey = CryptoUtils.loadPrivateKey(username);

                    JSONObject login = new JSONObject();

                    login.put("type", "login");
                    login.put("username", username);
                    login.put("password", password);

                    JsonUtil.sendJson(socket, login);

                    break;

                // ======================
                // SEND MESSAGE
                // ======================
                case "3":

                    if (privateKey == null) {
                        System.out.println("Login first.");
                        break;
                    }

                    System.out.print("To: ");
                    String target = sc.nextLine();

                    System.out.print("Message: ");
                    String text = sc.nextLine();

                    // ask for public key
                    JSONObject ask = new JSONObject();

                    ask.put("type", "get_pubkey");
                    ask.put("username", target);

                    JsonUtil.sendJson(socket, ask);

                    JSONObject resp = JsonUtil.recvJson(socket);

                    if (!resp.has("public_key")) {
                        System.out.println("User not found.");
                        break;
                    }

                    PublicKey pub =
                            CryptoUtils.deserializePublicKey(
                                    resp.getString("public_key")
                            );

                    EncryptedMessage enc =
                            CryptoUtils.encrypt(pub, text);

                    JSONObject msg = new JSONObject();

                    msg.put("type", "send");
                    msg.put("to", target);
                    msg.put("wrapped_key", enc.wrappedKey);
                    msg.put("nonce", enc.nonce);
                    msg.put("ciphertext", enc.ciphertext);

                    JsonUtil.sendJson(socket, msg);

                    break;

                // ======================
                // EXIT
                // ======================
                case "4":

                    socket.close();
                    sc.close();

                    System.out.println("Disconnected.");

                    return;

                default:
                    System.out.println("Invalid option");
            }
        }
    }

    // ======================
    // RECEIVER THREAD
    // ======================

    private static void startReceiver(Socket socket) {

        new Thread(() -> {

            try {

                while (true) {

                    JSONObject msg =
                            JsonUtil.recvJson(socket);

                    if (msg == null)
                        break;

                    String type = msg.optString("type");

                    if (type.equals("incoming")) {

                        String text =
                                CryptoUtils.decrypt(
                                        privateKey,
                                        new EncryptedMessage(
                                                msg.getString("wrapped_key"),
                                                msg.getString("nonce"),
                                                msg.getString("ciphertext")
                                        )
                                );

                        System.out.println(
                                "\n[" +
                                msg.getString("from") +
                                "] " +
                                text
                        );
                    }
                    else {

                        System.out.println(msg.toString(2));

                    }
                }

            } catch (Exception e) {

                System.out.println("Connection closed.");

            }

        }).start();
    }
}