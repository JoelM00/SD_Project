import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Stub {
    private final TaggedConnection tc;
    private final Socket s;
    private final Queue<String> ListaAtiva;
    private final List<String> ListaPassiva;
    private final ReentrantLock l;
    private final Condition cAtivo;

    public Stub() throws Exception {
        this.s = new Socket("localhost", 12345);
        this.tc = new TaggedConnection(s);
        this.ListaAtiva = new PriorityQueue<>();
        this.ListaPassiva = new ArrayList<>();
        this.l = new ReentrantLock();
        this.cAtivo = l.newCondition();
    }

    /**
     * Lê continuamente respostas do socket e coloca-as nas Listas apropriadas
     * no caso de ser uma resposta de pedido ativo coloca na Queue Lista Ativa e avisa o cliente que a resposta chegou
     * no caso de ser uma notificação passiva coloca somente na queue Lista passiva.
     */
    public void startReading () {
       new Thread(() -> {
            TaggedConnection.Frame f;
            try (this.tc) {
                while (true) {
                    f = tc.receive();
                    int tag = f.tag;
                    StringBuilder res = new StringBuilder();
                    switch (tag) {
                        case -1 -> res.append(new String(f.dados));
                        case 6 -> res.append(" -> Registo de infeção concluido, as melhoras!");
                        case 7 -> res.append(" -> Pessoas: ").append(new String(f.dados));
                        case 8 -> res.append(" -> Você tem risco de Infeção!");
                        case 9 -> {
                            res.append(" -> Mapa: ");
                            res.append(new String(f.dados));
                        }
                        case 10 -> {
                            res.append(" -> Registo Posicao: ");
                            res.append(new String(f.dados));
                        }
                        case 11 -> {
                            res.append(" -> Aviso Confirmado: ");
                            res.append(new String(f.dados));
                        }
                        case 13 -> res.append("->Posicao").append(new String(f.dados));
                        case 14 -> res.append(new String(f.dados));
                    }
                    try {
                        l.lock();
                        if (tag==8 || tag==14) {
                            ListaPassiva.add(res.toString());
                        } else {
                            ListaAtiva.add(res.toString());
                            cAtivo.signalAll();
                        }
                    } finally {
                        l.unlock();
                    }
                }
            } catch (Exception e) {
                System.out.println(" -> Saindo de Leitura");
            }
        }).start();
    }

    /**
     * Cliente bloqueia até que a resposta/s ao seu pedido cheguem (signal feito pelo startReading())
     * @param n numero de mensagens que o cliente esta a espera, pois há pedidos que podem devolver mais do que uma mns
     * @return String
     */
    public String esperaAtiva (int n){
        StringBuilder res= new StringBuilder();
        try {
            l.lock();
            while (ListaAtiva.size() <= (n-1)) {
                cAtivo.await();
            }
            for (int i=0;i<n;i++) {
                res.append(ListaAtiva.poll());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            l.unlock();
        }
        return res.toString();
    }

    /**
     * Enviar pedido pedido de autenticacao ou registo ao servidor
     * @param opcao autenticação => opção = 1,registar => opção=2,registarModerador => opcao = 3
     * @param nome nome
     * @param pass password
     * @return 0 = Username Taken / 1 = Registado com sucesso / 2 = Esta Infetado / 3 = Username Não Encontrado
     *         4 = Sucesso Proseguir / 5 = Pass Errada / 12 = já online
     */
    public int autenticarOrRegistrar (int opcao,String nome,String pass) {
        try {
            l.lock();
            tc.send(opcao, nome.concat(" ").concat(pass).getBytes());
            TaggedConnection.Frame f = tc.receive();
            if (f.tag == 4) {
                startReading(); //se registado com sucesso começa a ler repetidamente respostas do servidor pelo socket
            }
            return f.tag;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            l.unlock();
        }
        return -1;
    }

    /**
     * Envia pedido para atualizar login do utilizador com username = finalUsername
     * @param finalUsername nome
     * @return String mensagem de resposta
     */
     public String trataTerminaConexao(String finalUsername) {
        try {
            tc.send(0, (finalUsername).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return esperaAtiva(1);//espera ativamente por 1 mns do servidor
    }

    /**
     * Envia pedido para adicionar Posicao em (x,y) no cliente com username = nome
     * @param x coordenada
     * @param y coordenada
     * @param nome nome
     * @return String mensagem de resposta
     */
    public String trataAddPosicao (int x,int y,String nome) {
        try {
            tc.send(4, (nome + " " + x + " " + y).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return esperaAtiva(1);
    }

    /**
     * Envia pedido para registrar infeção no cliente com username = nome
     * @param nome nome
     * @return String mensagem de resposta
     */
    public String trataSetInfetado(String nome){
        try {
            tc.send(5, nome.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }
        return esperaAtiva(2);    //espera por 2 mns do servidor(infectado verificação e logOut)
    }

    /**
     * Envia pedido para contar quantas pessoas estão na posiçao (x,y)
     * @param x coordenada
     * @param y coordenada
     * @return String mensagem de resposta
     */
    public String trataGetPessoas (int x,int y){
        try {
            tc.send(6, (x + "," + y).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return esperaAtiva(1);
    }

    /**
     * Envia pedido para receber Mapa, é necessário nome para que só moderadores o podem receber
     * @param nome nome
     * @return String mensagem de resposta
     */
    public String trataMapa (String nome){
        try {
            tc.send(7,nome.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return esperaAtiva(1);
    }

    /**
     * Envia pedido para resetar aviso, ou seja confirmar que recebeu a mns de aviso
     * @param nome nome
     * @return String mensagem de resposta
     */
    public String trataRecepcaoAviso(String nome){
        try {
            tc.send(8,nome.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return esperaAtiva(1);
    }

    /**
     * Envia pedido para esperar por posicao vazia
     * @param x coordenada
     * @param y coordenada
     * @param nome nome
     * @return mensagem de resposta
     */
    public String trataEsperarPosicaoVazia(int x,int y,String nome) {
        try {
            tc.send(9, (nome + " " + x + " " + y).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return esperaAtiva(1);
    }

    /**
     * Ver todas as notificações recebidas
     * @return Lista de mensagens
     */
    public List<String> verMns (){
       List<String> res = new ArrayList<>();
       try {
           l.lock();
           res.addAll(ListaPassiva);
       } finally{
           l.unlock();
       }
       return res;
    }

    /**
     * Limpa lista passiva
     */
    public void clearMns(){
        this.ListaPassiva.clear();
    }

    /**
     * Retorna numero de mensagens
     * @return int
     */
    public int quantasMns (){
        return ListaPassiva.size();

    }
}

/*Recebe
  flag 0 = Username Taken
  flag 1 = Registado com sucesso
  flag 2 = Esta Infetado
  flag 3 = Username Não Encontrado
  flag 4 = Sucesso Proseguir
  flag 5 = Pass Errada
  flag 12 = já online
flag 6 = Registo Infetado Sucesso
flag 7 = Numero de Pessoal Em Posição
flag 8 = Aviso de possivel Infeção
flag 9 = Mapa de Morador
flag 10= Posicao registrada com sucesso
flag 11= Aviso Confirmado
flag 13= Recebe mns do servidor a dizer que irá ser avisado caso posição fique vazia
flag 14= Receb aviso que posição se encontra vazia
 */

/*Envia
 *  flag 1 -> int autenticar(String nome, String pass)
 *  flag 2 -> boolean registar(String nome, String pass)
 *  flag 3 -> boolean registarModerador (String nome,String pass)
 *  flag 4 -> void addPosicao(String nome, Posicao posicao)
 *  flag 5 -> int setInfectadoTrue(String cliente)
 *  flag 6 -> int numeroPessoasPosicao(Posicao posicao)
 *  flag 7 -> public String mapaModerador()
 *  flag 8 -> public String ConfirmarAviso
 *  flag 9 -> public espera Por Posicao Vazia*/
