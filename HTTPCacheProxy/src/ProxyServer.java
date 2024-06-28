import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer {
    public static void main(String[] args) {
        int port = 8888;
        new ProxyServer().startServer(port);
    }

    public void startServer(int port) {
        try(ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Proxy Server started on port " + port);
            while (true){
                Socket clientSocket = serverSocket.accept();
                new Thread(new ProxyClientHandler(clientSocket)).start();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
