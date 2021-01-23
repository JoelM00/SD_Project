import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Dados {
    /**
     * Variaveis de instancia
     */
    private final Map<String,Cliente> clientes;
    private final ReentrantLock l;
    private final Condition c;
    private final List<Posicao> listaEsperaVazia;

    /**
     * Construtor
     */
    public Dados() {
        this.l = new ReentrantLock();
        this.clientes = new HashMap<>();
        this.c = l.newCondition();
        this.listaEsperaVazia = new ArrayList<>();
    }

    /**
     * Bloqueia o objeto
     */
    public void lock (){ this.l.lock(); }

    /**
     * Desbloqueia o objeto
     */
    public void unlock(){ this.l.unlock(); }

    /**
     * Adormece sobre o objeto
     */
    public void await() throws InterruptedException { this.c.await();}

    /**
     * Adiciona posicao a lista.
     * @param p Posicao
     */
    public void avisaPosicao (Posicao p) { this.listaEsperaVazia.add(p);}

    /**
     * Acorda adormecidos sobre o objeto
     */
    public void signalAll (){ this.c.signalAll();}

    /**
     * Remove posicao da lista
     * @param p Posicao
     */
    public void listaEsperaRemove (Posicao p) { listaEsperaVazia.remove(p);}

    /**
     * Conta o numero de pessoas numa dada coordenada
     * @param x coordenada
     * @param y coordenada
     * @return int
     */
    public int peopleInPosition(int x,int y){
        Posicao p = new Posicao(x,y);
        int total = 0;
        Cliente cli;

        try {
            this.l.lock();

            for (Map.Entry<String, Cliente> aux : this.clientes.entrySet()) {
                cli = aux.getValue();
                try {
                    cli.lock();
                    if (cli.isInPosition(p)) total++;
                } finally {
                    cli.unlock();
                }
            }
        } finally {
            this.l.unlock();
        }
        return total;
    }

    /**
     * Verifica se a posicao passada esta na lista
     * @param p Posicao
     * @return boolean
     */
    public boolean listaEsperaVaziaContains(Posicao p){
        return this.listaEsperaVazia.contains(p);
    }

    /**
     * Imprime o estado dos clientes
     */
    public void printEstado() {
        Cliente cli;
        try {
            this.l.lock();

            for (Map.Entry<String, Cliente> aux : this.clientes.entrySet()) {
                cli = aux.getValue();
                cli.lock();
                try {
                    System.out.println(cli.toString());
                } finally {
                    cli.unlock();
                }
            }
        } finally {
            this.l.unlock();
        }
    }

    /**
     * Regista um novo cliente
     * @param nome nome
     * @param pass password
     * @return int
     */
    public int registar(String nome, String pass) {
        int res;
        try {
            this.l.lock();
            if (this.clientes.containsKey(nome)) res = 0;
            else {
                this.clientes.put(nome, new Cliente(nome, pass));
                res = 1;
            }
        } finally {
            this.l.unlock();
        }
        return res;
    }

    /**
     * Regista um novo cliente moderador
     * @param nome nome
     * @param pass password
     * @return int
     */
    public int registarModerador(String nome, String pass) {
        int res;
        try {
            this.l.lock();
            if (this.clientes.containsKey(nome)) res = 0;
            else {
                Cliente client = new Cliente(nome, pass);
                client.setModerador(true);
                this.clientes.put(nome, client);
                res = 1;
            }
        } finally {
            this.l.unlock();
        }
        return res;
    }

    /**
     * Autentica um cliente
     * @param nome nome
     * @param pass password
     * @return int
     */
    public int autenticar(String nome, String pass) {
       Cliente cli;
       int res;
       try {
           this.l.lock();
           if (this.clientes.containsKey(nome)) {
               cli = this.clientes.get(nome);
               try {
                   cli.lock();
                   if (cli.isInfectado()) {
                       res = -1;

                   } else if (cli.isLogin()) {
                       res = 3;

                   } else if (cli.getPassword().equals(pass)) {
                       cli.setLogin(true);
                       res = 1;

                   } else res = 2;

               } finally {
                   cli.unlock();
               }

           } else {
               res = 0;
           }
       } finally {
           this.l.unlock();
       }
       return res;
    }

    /**
     * Redefine estado de infecao de um cliente
     * @param nome nome
     * @param b estado de infecao
     */
    public void setInfectado(String nome,boolean b) {
        Cliente ori;
        List<Posicao> esteveEm;
        Cliente cli;
        try {
            l.lock();
            ori = this.clientes.get(nome);
            try {
                ori.lock();
                esteveEm = this.clientes.get(nome).getPosicoes();
                ori.setInfectado(b);
                ori.removeAllPositions();
            } finally {
                ori.unlock();
            }

            for (Map.Entry<String, Cliente> aux : this.clientes.entrySet()) {
                cli = aux.getValue();
                try {
                    cli.lock();
                    if (!cli.isInfectado() && !cli.getAviso()) {
                        for (Posicao pos : cli.getPosicoes()) {
                            if (esteveEm.contains(pos)) {
                                cli.setAviso(true);
                                cli.signalAll();
                                break;
                            }
                        }
                    }
                } finally {
                    cli.unlock();
                }
            }
        } finally {
            l.unlock();
        }
    }

    /**
     * Retorna o cliente com dado nome
     * @param nome nome
     * @return Cliente
     */
    public Cliente getCliente(String nome) {
        try {
            l.lock();
            return this.clientes.get(nome);
        } finally {
            l.unlock();
        }
    }

    /**
     * Converte o mapa a uma string
     * @return String
     */
    public String mapaModerador(){
        StringBuilder s= new StringBuilder();
        Map<Posicao,Integer> infectados = new HashMap<>();
        Map<Posicao,Integer> visitantes = new HashMap<>();
        Cliente cli;
        int xMax=-1; int yMax=-1;
        try {
            l.lock();
            for (Map.Entry<String, Cliente> aux : this.clientes.entrySet()) {
                cli = aux.getValue();
                cli.lock();
                try {
                    for (Posicao p : cli.getPosicoes()) {
                        if (p.x > xMax) xMax = p.x;
                        if (p.y > yMax) yMax = p.y;
                        if (cli.isInfectado()) {
                            if (infectados.containsKey(p)) {
                                infectados.put(p, infectados.get(p) + 1);
                            } else {
                                infectados.put(p, 1);
                            }
                        } else {
                            if (visitantes.containsKey(p)) {
                                visitantes.put(p, visitantes.get(p) + 1);
                            } else {
                                visitantes.put(p, 1);
                            }
                        }
                    }
                } finally {
                    cli.unlock();
                }
            }
        } finally {
            l.unlock();
        }
        if (xMax==-1 && yMax==-1) return "# -> Mapa Vazio";
        s.append("\n");
        for (int i=yMax; i>=0; i--) {
            for (int j=0; j<=xMax; j++) {
                Posicao p = new Posicao (j,i);
                s.append("(X=").append(j).append(",Y=").append(i).append(")=>");
                s.append("(I=").append(infectados.getOrDefault(p, 0));
                s.append(",V=").append(visitantes.getOrDefault(p, 0)).append(")");
                s.append(" || ");
            }
            s.append("\n");
        }
        return s.toString();
    }
}

