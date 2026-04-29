import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

// Most of the code here was provided by professor Domen Vake

public class HttpsFetcher {           // Fetches the HTML of a page
    public String fetch (String host, String path) throws IOException {

        int port = 443;
        if (path == null || path.isEmpty()) path = "/";

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = null;

        try {
            socket = (SSLSocket) factory.createSocket(host, port);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException("Failed to create SSL Socket.",ex);
        }


        socket.startHandshake();

        // Send HTTP GET
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        out.print("GET " + path + " HTTP/1.1\r\n");
        out.print("Host: " + host + "\r\n");
        out.print("Connection: close\r\n");
        out.print("\r\n");
        out.flush();

        // Read response
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line).append("\n");
        }

        socket.close();
        return sb.toString();
    }
}
