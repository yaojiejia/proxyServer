import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class ProxyClientHandler implements Runnable{
    private final Socket clientSocket;
    private static final ProxyCache cache = new ProxyCache();

    public ProxyClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            InputStream clientInput = clientSocket.getInputStream();
            OutputStream clientOutput = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientInput));
            PrintWriter writer = new PrintWriter(clientOutput);
            String requestLine = reader.readLine();
            if (requestLine != null && !requestLine.isEmpty()) {
                System.out.println("Request: " + requestLine);
                String[] requestParts = requestLine.split(" ");
                System.out.println("requests part 0 is " + requestParts[0]);
                if (requestParts[0].equals("GET")){
                    handleGetRequest(requestParts[1], writer);
                }
                else if (requestParts[0].equals("CONNECT")) {
                    handleConnectRequest(requestParts[1], clientInput, clientOutput);
                }
                else {
                    writer.println("HTTP/1.1 400 Bad Request");
                    writer.println("Content-Length: 0");
                    writer.println();
                    writer.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleGetRequest(String url, PrintWriter writer){
        System.out.println("Handling GET request for " + url);
        String cachedResponse = cache.getCacheResponse(url);
        if(cachedResponse != null){
            writer.println("Cache hit for " + url);
            writer.println(cachedResponse);
            writer.flush();
            return;
        }

        try{
            URL targetURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) targetURL.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            InputStream targetInput = connection.getInputStream();
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("HTTP/1.1 ").append(responseCode).append(" ").append(connection.getResponseMessage()).append("\n");
            try(BufferedReader targetReader = new BufferedReader(new InputStreamReader(targetInput))){
                String line;
                while ((line = targetReader.readLine()) != null){
                    responseBuilder.append(line).append("\n");
                }
            }
            writer.println(responseBuilder);
            writer.println("catch miss for " + url);
            cache.cacheResponse(url, responseBuilder.toString());

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void handleConnectRequest(String hostAndPort, InputStream clientInput, OutputStream clientOutput) {
        try {
            String[] parts = hostAndPort.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            Socket serverSocket = new Socket();
            serverSocket.connect(new InetSocketAddress(host, port));

            PrintWriter writer = new PrintWriter(clientOutput);
            writer.println("HTTP/1.1 200 Connection established");
            writer.println("Proxy-agent: Simple-Proxy/1.0");
            writer.println();
            writer.flush();

            Thread clientToServer = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        relayData(clientInput, serverSocket.getOutputStream());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            Thread serverToClient = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        relayData(serverSocket.getInputStream(), clientOutput);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });

            clientToServer.start();
            serverToClient.start();

            clientToServer.join();
            serverToClient.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void relayData(InputStream input, OutputStream output) {
        try {
            ReadableByteChannel inputChannel = Channels.newChannel(input);
            WritableByteChannel outputChannel = Channels.newChannel(output);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (inputChannel.read(buffer) != -1) {
                buffer.flip();
                outputChannel.write(buffer);
                buffer.compact();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
