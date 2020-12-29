
import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class TaggedConnection implements Closeable {
    private Socket s;
    private DataInputStream in;
    private DataOutputStream out;
    private ReentrantLock wl;
    private ReentrantLock rl;

    public static class Frame {
        public int tag;
        public byte[] dados;

        public Frame(int tag,byte[] dados) {
            this.tag = tag;
            this.dados = dados;
        }
    }

    public TaggedConnection(Socket s) throws Exception {
        this.s = s;
        this.in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        this.wl = new ReentrantLock();
        this.rl = new ReentrantLock();
    }

    public void send(Frame f) throws Exception {
        this.send(f.tag,f.dados);
    }

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

    @Override
    public void close() throws IOException {
        this.s.close();
    }
}
