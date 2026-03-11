import java.net.Socket;
import java.security.*;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;

public class ClientMain {

    private static PrivateKey privateKey;
    private static String username;

    private static BlockingQueue<JSONObject> responseQueue =
            new LinkedBlockingQueue<>();

    public static void main(String[] args) throws Exception {

        Socket socket = new Socket("127.0.0.1", 12345);
        Scanner sc = new Scanner(System.in);

        startReceiver(socket);

        while (true) {

            System.out.println("\n1 Register");
            System.out.println("2 Login");
            System.out.println("3 Send message");
            System.out.println("4 Exit");

            String choice = sc.nextLine();

            if (choice.equals("1")) {

                System.out.print("Username: ");
                username = sc.nextLine();

                System.out.print("Password: ");
                String password = sc.nextLine();

                KeyPair pair = CryptoUtils.generateRSAKeys();

                privateKey = pair.getPrivate();

                CryptoUtils.savePrivateKey(username, privateKey);

                JSONObject obj = new JSONObject();

                obj.put("type", "register");
                obj.put("username", username);
                obj.put("password", password);
                obj.put("public_key",
                        CryptoUtils.serializePublicKey(pair.getPublic()));

                JsonUtil.sendJson(socket, obj);

                System.out.println(responseQueue.take());
            }

            else if (choice.equals("2")) {

                System.out.print("Username: ");
                username = sc.nextLine();

                System.out.print("Password: ");
                String password = sc.nextLine();

                privateKey = CryptoUtils.loadPrivateKey(username);

                JSONObject obj = new JSONObject();

                obj.put("type", "login");
                obj.put("username", username);
                obj.put("password", password);

                JsonUtil.sendJson(socket, obj);

                System.out.println(responseQueue.take());
            }

            else if (choice.equals("3")) {

                if (privateKey == null) {
                    System.out.println("Login first.");
                    continue;
                }

                System.out.print("To: ");
                String target = sc.nextLine();

                System.out.print("Message: ");
                String text = sc.nextLine();

                JSONObject ask = new JSONObject();

                ask.put("type", "get_pubkey");
                ask.put("username", target);

                JsonUtil.sendJson(socket, ask);

                JSONObject resp = responseQueue.take();

                if (!resp.has("public_key")) {
                    System.out.println("User not found.");
                    continue;
                }

                PublicKey pub =
                        CryptoUtils.deserializePublicKey(
                                resp.getString("public_key"));

                EncryptedMessage enc =
                        CryptoUtils.encrypt(pub, text);

                JSONObject msg = new JSONObject();

                msg.put("type", "send");
                msg.put("to", target);
                msg.put("wrapped_key", enc.wrappedKey);
                msg.put("nonce", enc.nonce);
                msg.put("ciphertext", enc.ciphertext);

                JsonUtil.sendJson(socket, msg);

                System.out.println(responseQueue.take());
            }

            else if (choice.equals("4")) {

                socket.close();
                sc.close();
                break;
            }
        }
    }

    private static void startReceiver(Socket socket) {

        new Thread(() -> {

            try {

                while (true) {

                    JSONObject msg = JsonUtil.recvJson(socket);

                    if (msg == null)
                        break;

                    if (msg.getString("type").equals("incoming")) {

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
                                "\n[" + msg.getString("from") + "] " + text
                        );

                    } else {

                        responseQueue.put(msg);
                    }
                }

            } catch (Exception e) {

                System.out.println("Connection closed.");

            }

        }).start();
    }
}