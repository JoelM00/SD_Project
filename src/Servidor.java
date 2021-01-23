
import java.net.ServerSocket;
import java.net.Socket;


public class Servidor {
    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket(12345);
        Dados dados = new Dados();

        while (true) {
            Socket s = ss.accept();
            TaggedConnection c = new TaggedConnection(s);
            new Thread(new ServerWorker(c,dados)).start();
        }
    }
}