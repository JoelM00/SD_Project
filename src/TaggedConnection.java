
import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class TaggedConnection implements Closeable {
    /**
     * Variaveis de instancia
     */
    private final Socket s;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final ReentrantLock wl;
    private final ReentrantLock rl;

    /**
     * Classe auxiliar
     */
    public static class Frame {
        public int tag;
        public byte[] dados;

        public Frame(int tag,byte[] dados) {
            this.tag = tag;
            this.dados = dados;
        }
    }

    /**
     * Construtor parametrizado
     * @param s
     * @throws IOException Erro de Input/Output
     */
    public TaggedConnection(Socket s) throws IOException {
        this.s = s;
        this.in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        this.wl = new ReentrantLock();
        this.rl = new ReentrantLock();
    }

    /**
     * Escritor de informacoes no socket
     * @param tag identificador
     * @param dados bytes de dados
     * @throws IOException Erro de Input/Output
     */
    public void send(int tag,byte[] dados) throws IOException {
        try {
            wl.lock();
            out.writeInt(tag);
            out.writeInt(dados.length);
            out.write(dados);
            out.flush();
        } finally {
            wl.unlock();
        }
    }

    /**
     * Leitor de informacoes no socket
     * @return Frame
     * @throws IOException Erro de Input/Output
     */
    public Frame receive() throws IOException {
        try {
            rl.lock();
            int tag = in.readInt();
            int size = in.readInt();
            byte[] dados = new byte[size];
            in.readFully(dados);
            return new Frame(tag,dados);
        } finally {
            rl.unlock();
        }
    }

    /**
     * Fecha socket
     * @throws IOException Erro de Input/Output
     */
    @Override
    public void close() throws IOException {
        this.s.close();
    }
}
