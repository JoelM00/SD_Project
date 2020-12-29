import java.net.ServerSocket;
import java.net.Socket;


public class Servidor {
    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket(12345);
        Dados dados = new Dados();

        while (true) {
            Socket s = ss.accept();
            TaggedConnection c = new TaggedConnection(s);

            Runnable worker = () -> {
                try (c) {
                    while (true) {
                        TaggedConnection.Frame frame = c.receive();
                        int tag = frame.tag;
                        byte[] informacao = frame.dados;
                        int res;

                        switch (tag) {
                            case 0 : // registar
                                res = dados.registar(informacao);
                                System.out.println(" -> "+res);
                                if (res == 0) {
                                   c.send(0,"Username usado!".getBytes());
                                } else {
                                    c.send(0,"Sucesso".getBytes());
                                }
                                break;
                            case 1 :
                                res = dados.autenticar(informacao);
                                System.out.println(" -> "+res);
                                switch (res) {
                                    case -1 -> c.send(0,"Esta infectado!".getBytes());
                                    case 0 -> c.send(0,"Username nao econtrado!".getBytes());
                                    case 1 -> c.send(0,"Sucesso!".getBytes());
                                    case 2 -> c.send(0,"Pass errada!".getBytes());
                                }
                                break;
                            case 2 :
                                dados.addPosicao(informacao);
                                System.out.println(" -> Posicao adicionada!");
                            case 3 :
                                dados.setInfectadoTrue(informacao);
                                System.out.println(" -> Infectado!");
                                c.send(3,"Adeus! As melhoras!".getBytes());
                            default: break;
                        }

                        dados.printEstado();
                    }
                } catch (Exception ignored) {
                    System.out.println("Conexao terminada!");
                }
            };
            new Thread(worker).start();
        }
    }
}


/* flag 0 -> boolean registar(String nome, String pass)
*  flag 1 -> int autenticar(String nome, String pass)
*  flag 2 -> void addPosicao(String nome, Posicao posicao)
*  flag 3 -> int setInfectadoTrue(String cliente)
*  flag 4 -> int numeroPessoasPosicao(Posicao posicao)*/