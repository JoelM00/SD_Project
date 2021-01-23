import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Cliente {
    /**
     * Variaveis de instancia
     */
    private String nome;
    private String password;
    private boolean infectado;
    private List<Posicao> posicoes;
    private boolean aviso;
    private boolean moderador;
    private boolean login;
    private final ReentrantLock l;
    private final Condition c;

    /**
     * Construtor parametrizado
     * @param nome nome
     * @param password password
     */
    public Cliente(String nome, String password) {
        this.nome = nome;
        this.password = password;
        this.infectado = false;
        this.posicoes = new ArrayList<>();
        this.moderador = false;
        this.login = false;
        this.l = new ReentrantLock();
        this.c = l.newCondition();
    }

    /**
     * Funcoes auxiliares de acesso
     */
    public boolean getModerador() {return this.moderador;}
    public boolean getAviso() { return this.aviso; }
    public String getNome() { return nome; }
    public boolean isInfectado() { return infectado; }
    public String getPassword() { return password; }
    public boolean isAviso() { return aviso; }
    public boolean isModerador() { return moderador; }
    public boolean isLogin() { return login; }
    public void setNome(String nome) { this.nome = nome; }
    public void setPassword(String password) { this.password = password; }
    public void setInfectado(boolean infectado) { this.infectado = infectado; }
    public void setPosicoes(List<Posicao> posicoes) { this.posicoes = posicoes; }
    public void setLogin(boolean login) { this.login = login; }
    public void setAviso(boolean aviso) { this.aviso = aviso; }
    public void setModerador(boolean moderador) {this.moderador=moderador;}
    public void addPosicao(Posicao p) { posicoes.add(p);}
    public void lock() {l.lock(); }
    public void unlock(){l.unlock();}
    public void signalAll () {c.signalAll();}
    public void await() throws InterruptedException{c.await();}

    /**
     * Limpa lista de posicoes
     */
    public void removeAllPositions(){
        this.posicoes.clear();
    }

    /**
     * Retorna lista de posicoes
     * @return List
     */
    public List<Posicao> getPosicoes() {
        List<Posicao> res = new ArrayList<>();
        for(Posicao p : this.posicoes){
            res.add(p.clone());
        }
        return res;
    }

    /**
     * Verifica se esta ou nao na posicao
     * @param p posicao
     * @return bolean
     */
    public boolean isInPosition (Posicao p){
        if (!this.posicoes.isEmpty()) {
            return this.posicoes.get(posicoes.size() - 1).equals(p); //compara P com a ultima posicao
        } else return false;
    }

    /**
     * Retorna a posicao do cliente.
     * @return Posicao
     */
    public Posicao posicionCliente (){
        if (!this.posicoes.isEmpty()) {
            return this.posicoes.get(posicoes.size() - 1);
        } else return null;
    }

    /**
     * Retorna string do objeto
     * @return String
     */
    @Override
        public String toString() {
            return "Cliente{" +
                    "nome=" + nome +
                    ", password=" + password  +
                    ", infectado=" + infectado +
                    ", aviso="+aviso +
                    ", posicoes=" + posicoes.toString() +
                    ", moderador=" + moderador +
                    ", login=" + login +
                    '}';
        }
}
