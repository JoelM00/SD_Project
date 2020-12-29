import java.util.*;

public class Dados {
    private Map<String,Cliente> clientes;

    public Dados() {
        this.clientes = new HashMap<>();
    }

    private class Posicao {
        public int x;
        public int y;

        public Posicao(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Posicao{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    private class Cliente {
        public String nome;
        public String password;
        public boolean infectado;
        public List<Posicao> posicoes;

        public Cliente(String nome, String password) {
            this.nome = nome;
            this.password = password;
            this.infectado = false;
            this.posicoes = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "Cliente{" +
                    "nome=" + nome +
                    ", password=" + password  +
                    ", infectado=" + infectado +
                    ", posicoes=" + posicoes.toString() +
                    '}';
        }
    }

    public void addPosicao(String cliente,Posicao p) {
        this.clientes.get(cliente).posicoes.add(p);
    }

    public void setInfectado(String cliente, boolean b) {
        this.clientes.get(cliente).infectado = b;
    }

    public void setInfectadoTrue(byte[] bytes) {
        String s = new String(bytes);
        this.setInfectado(s,true);
    }

    public int autenticar(String cliente, String pass) {
        if (this.clientes.containsKey(cliente)) {
            if (this.clientes.get(cliente).infectado) return -1;
            if (this.clientes.get(cliente).password.equals(pass)) return 1;
            else return 2;
        } else return 0;
    }

    public int autenticar(byte[] bytes) {
        String s = new String(bytes);
        String[] temp = s.split(" ",2);
        return autenticar(temp[0],temp[1]);
    }


    public int registar(String cliente, String pass) {
        if (this.clientes.containsKey(cliente)) return 0;
        else {
            this.clientes.put(cliente, new Cliente(cliente, pass));
            return 1;
        }
    }

    public int registar(byte[] bytes) {
        String s = new String(bytes);
        String[] temp = s.split(" ",2);
        return registar(temp[0],temp[1]);
    }

    public void printEstado() {
        for (Map.Entry<String,Cliente> aux : this.clientes.entrySet()) {
            System.out.println(aux.getValue().toString());
        }
    }

    public void addPosicao(byte[] bytes) {
        String s = new String(bytes);
        String[] temp = s.split(" ",3);
        Posicao p = new Posicao(Integer.parseInt(temp[1]),Integer.parseInt(temp[2]));

        addPosicao(temp[0],p);
    }

}
