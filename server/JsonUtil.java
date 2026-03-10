import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class JsonUtil {

    public static void sendJson(Socket socket, JSONObject obj) throws Exception {

        byte[] raw = obj.toString().getBytes();

        DataOutputStream out =
                new DataOutputStream(socket.getOutputStream());

        out.writeInt(raw.length);
        out.write(raw);
        out.flush();
    }

    public static JSONObject recvJson(Socket socket) throws Exception {

        DataInputStream in =
                new DataInputStream(socket.getInputStream());

        int length = in.readInt();

        byte[] raw = new byte[length];

        in.readFully(raw);

        return new JSONObject(new String(raw));
    }
}