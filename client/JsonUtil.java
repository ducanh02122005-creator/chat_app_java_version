import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.json.JSONObject;

public class JsonUtil {

    // ======================
    // SEND JSON
    // ======================

    public static void sendJson(Socket socket, JSONObject obj)
            throws Exception {

        byte[] raw = obj.toString().getBytes();

        OutputStream out = socket.getOutputStream();

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(raw.length);

        out.write(buffer.array());
        out.write(raw);
        out.flush();
    }

    // ======================
    // RECEIVE JSON
    // ======================

    public static JSONObject recvJson(Socket socket)
            throws Exception {

        InputStream in = socket.getInputStream();

        byte[] lenBytes = recvall(in, 4);

        int length =
                ByteBuffer.wrap(lenBytes).getInt();

        byte[] raw = recvall(in, length);

        return new JSONObject(new String(raw));
    }

    // ======================
    // RECEIVE FULL DATA
    // ======================

    private static byte[] recvall(InputStream in, int n)
            throws Exception {

        byte[] data = new byte[n];

        int total = 0;

        while (total < n) {

            int r = in.read(data, total, n - total);

            if (r == -1)
                throw new Exception("Connection closed");

            total += r;
        }

        return data;
    }
}