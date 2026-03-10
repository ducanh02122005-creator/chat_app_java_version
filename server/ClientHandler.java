import org.json.JSONObject;

import java.net.Socket;

public class ClientHandler implements Runnable {

    Socket socket;
    String username = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {

        try {

            while (true) {

                JSONObject msg =
                        JsonUtil.recvJson(socket);

                String type = msg.getString("type");

                if (type.equals("register")) {

                    boolean ok = UserManager.register(
                            msg.getString("username"),
                            msg.getString("password"),
                            msg.getString("public_key")
                    );

                    JSONObject res = new JSONObject();
                    res.put("type", ok ? "ok" : "fail");

                    JsonUtil.sendJson(socket, res);
                }

                else if (type.equals("login")) {

                    boolean ok = UserManager.login(
                            msg.getString("username"),
                            msg.getString("password"),
                            socket
                    );

                    if (ok)
                        username = msg.getString("username");

                    JSONObject res = new JSONObject();
                    res.put("type", ok ? "ok" : "fail");

                    JsonUtil.sendJson(socket, res);
                }

                else if (type.equals("get_pubkey")) {

                    User u = UserManager.getUser(
                            msg.getString("username")
                    );

                    if (u != null) {

                        JSONObject res = new JSONObject();

                        res.put("type", "pubkey");
                        res.put("public_key", u.publicKey);

                        JsonUtil.sendJson(socket, res);
                    }
                }

                else if (type.equals("send")) {

                    User target =
                            UserManager.getUser(msg.getString("to"));

                    if (target != null && target.socket != null) {

                        JSONObject incoming = new JSONObject();

                        incoming.put("type", "incoming");
                        incoming.put("from", username);
                        incoming.put("wrapped_key",
                                msg.getString("wrapped_key"));
                        incoming.put("nonce",
                                msg.getString("nonce"));
                        incoming.put("ciphertext",
                                msg.getString("ciphertext"));

                        JsonUtil.sendJson(
                                target.socket,
                                incoming
                        );

                        JSONObject ok = new JSONObject();
                        ok.put("type", "ok");

                        JsonUtil.sendJson(socket, ok);
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            if (username != null)
                UserManager.logout(username);

            try {
                socket.close();
            } catch (Exception ignored) {}
        }
    }
}