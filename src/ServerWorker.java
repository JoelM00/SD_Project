import java.io.IOException;

public class ServerWorker implements Runnable {
    /**
     * Variaveis de instancia
     */
    private final TaggedConnection c;
    private final Dados dados;

    /**
     * Construtor parametrizado
     * @param c TaggedConnection
     * @param dados Dados
     */
    public ServerWorker(TaggedConnection c,Dados dados) {
        this.c = c;
        this.dados = dados;
    }

    /**
     * Codigo a executar pela Thread
     */
    @Override
    public void run() {
        try (c) {
            boolean stop=false;
            while (!stop) {
                TaggedConnection.Frame frame = c.receive();
                int tag = frame.tag;
                byte[] informacao = frame.dados;

                switch (tag) {
                    case 0 -> {
                        logOut(dados,informacao,c);
                        stop = true;
                    }
                    case 1 -> autenticar(dados,informacao,c);
                    case 2 -> registar(dados,informacao,c);
                    case 3 -> registarModerador(dados,informacao,c);
                    case 4 -> addPosicao(dados,informacao,c);
                    case 5 -> {
                        setInfectadoTrue(dados, informacao, c);
                        logOut(dados, informacao, c);
                        stop = true;
                    }
                    case 6 -> peopleInPosition(dados,informacao,c);
                    case 7 -> mapa(dados,informacao,c);
                    case 8 -> confirmaAviso(dados,informacao,c);
                    case 9 -> esperaPosicaoVazia(dados,informacao,c);
                }

                dados.printEstado();
                System.out.println(dados.mapaModerador());
            }
            System.out.println("$$$ - Conexao terminada! - $$$");
        } catch (Exception e) {
            System.out.println("$$$ - Conexao terminada! - $$$");
        }
    }

    /**
     * Remove cliente do sistema
     * @param dados Dados
     * @param bytes informacao
     * @param c TaggedConnection
     */
    private static void logOut(Dados dados,byte[] bytes,TaggedConnection c) {
        String userName = new String(bytes);
        System.out.println("# -> Fazendo logout de: "+userName);
        Cliente cli;
        try {
            dados.lock();
            cli = dados.getCliente(userName);
            cli.lock();
        } finally {
            dados.unlock();
        }
        try {
            cli.setLogin(false);
            cli.signalAll();
        } finally {
            cli.unlock();
        }
        try{
            c.send(-1, " -> Logout efetuado!\n".getBytes());
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Efetua o registo de um cliente
     * @param dados Dados
     * @param bytes informacao
     * @param c TaggedConnection
     */
    public static void registar (Dados dados,byte[] bytes,TaggedConnection c){
        String s = new String(bytes);
        String[] temp = s.split(" ",2);
        int res = dados.registar(temp[0], temp[1]);
        System.out.println("# -> Fazendo registo de: "+temp[0]);
        try {
            if (res == 0) {
                c.send(0, " -> Username usado!".getBytes());
            } else {
                c.send(1, " -> Registo bem sucedido!".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Efetua o registo de um cliente moderador
     * @param dados Dados
     * @param bytes informacao
     * @param c TaggedConnection
     */
    public static void registarModerador(Dados dados,byte[] bytes,TaggedConnection c){
        String s = new String(bytes);
        String[] temp = s.split(" ",2);
        int res = dados.registarModerador(temp[0], temp[1]);
        System.out.println("# -> Fazendo registo de: "+temp[0]);
        try {
            if (res == 0) {
                c.send(0, " -> Username usado!".getBytes());
            } else {
                c.send(1, " -> Registo bem sucedido!".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Efetua autenticacao de um cliente
     * @param dados Dados
     * @param bytes informacao
     * @param c TaggedConnection
     */
    public static void autenticar(Dados dados,byte[] bytes,TaggedConnection c) {
        String s = new String(bytes);
        String[] temp = s.split(" ",2);
        String nome = temp[0];
        String pass = temp[1];
        int res = dados.autenticar(nome, pass);
        System.out.println("# -> Autenticando: "+temp[0]);
        try {
            switch (res) {
                case -1 -> c.send(2, " -> Esta infectado!".getBytes());
                case 0 -> c.send(3, " -> Username nao econtrado!".getBytes());
                case 1 -> c.send(4, " -> Sucesso!".getBytes());
                case 2 -> c.send(5, " -> Pass errada!".getBytes());
                case 3 -> c.send(12," -> Sua Conta esta Online noutro dispositivo".getBytes());
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        if (res == 1) {
            checkarAviso(dados,c,nome);
        }
    }

    /**
     * Verifica constantemente a existencia de avisos para um determinado cliente
     * @param dados Dados
     * @param c TaggedConnection
     * @param nome nome
     */
    public static void checkarAviso(Dados dados,TaggedConnection c,String nome){
        new Thread(() -> {
            Cliente cli;
            try {
                dados.lock();
                cli = dados.getCliente(nome);
                cli.lock();
            } finally {
                dados.unlock();
            }
            try (c) {
                while (true) {
                    System.out.println("# -> Verificando avisos de: "+nome);
                    if (cli.getAviso()) {
                        System.out.println("# -> Risco de infeção: "+nome);
                        c.send(8, "É possivel Infeção".getBytes());
                    } else {
                        System.out.println("# -> Não está em risco: "+nome);
                    }
                    if (!cli.isLogin()) {
                        System.out.println("# -> Não esperando por aviso, user offline: "+nome);
                        break;
                    } else System.out.println("# -> Esperando por aviso, user online: "+nome);
                    cli.await();
                }
            } catch (Exception e) {
                System.out.println("# -> Terminando chekar avisos: "+nome);
            } finally {
                cli.unlock();
            }
            System.out.println("# -> Terminando chekar avisos: "+nome);
        }).start();
    }

    /**
     * Adiciona uma nova posicao a um dados cliente e reajusta mapa
     * @param dados Dados
     * @param bytes informacao
     * @param c TaggedConnection
     */
    public static void addPosicao(Dados dados,byte[] bytes,TaggedConnection c) {
        String s = new String(bytes);
        String[] temp = s.split(" ",3);
        int x = Integer.parseInt(temp[1]);
        int y = Integer.parseInt(temp[2]);
        Cliente cli;
        try {
            dados.lock();
            cli = dados.getCliente(temp[0]);
            cli.lock();
            Posicao p = cli.posicionCliente();
            if (p != null && dados.listaEsperaVaziaContains(p)) {
                dados.signalAll();
                dados.listaEsperaRemove(p);
            }
        } finally {
            dados.unlock();
        }
        try {
            cli.addPosicao(new Posicao(x,y));
        } finally {
            cli.unlock();
        }
        try {
            c.send(10,("Posicao ("+x+","+y+") Registrada com sucesso").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Espera que determinada posicao fica vazia
     * @param dados Dados
     * @param bytes informacao
     * @param c TaggedConnection
     */
    private static void esperaPosicaoVazia(Dados dados,byte[] bytes,TaggedConnection c) {
        String s = new String(bytes);
        String[] temp = s.split(" ", 3);
        int x = Integer.parseInt(temp[1]);
        int y = Integer.parseInt(temp[2]);
        String nome = temp[0];
        Cliente cli;
        int res;
        try {
            dados.lock();
            cli = dados.getCliente(nome);
            res = dados.peopleInPosition(x, y);
        } finally {
            dados.unlock();
        }
        try {
            if (res == 0) {
                c.send(13, (" A posição já se encontra vazia").getBytes());
            } else c.send(13, (" Tem "+res +" Pessoas. Quando A Posição ficar Vazia Irá receber uma Mns").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (res!=0) {
            new Thread(() -> {
                int pessoas;
                boolean login;
                try {
                    dados.lock();
                    try {
                        dados.avisaPosicao(new Posicao(x,y));
                        while ((pessoas=dados.peopleInPosition(x,y)) != 0) {
                            try{
                                cli.lock();
                                login = cli.isLogin();
                            } finally {
                                cli.unlock();
                            }
                            if (!login) break;
                            System.out.println("Existem: "+pessoas +" na posicao ("+x+","+y+");");
                            dados.await();
                        }
                        c.send(14, ("Posicão (" + x + "," + y + ")" + " Encontra-se Vazia ").getBytes());
                        System.out.println(" # -> Posicão (" + x + "," + y + ")" + " Encontra-se Vazia ");
                    } catch (Exception e) {
                        System.out.println("# -> Terminando Esperar Posicao Vazia " + nome);
                    }
                } finally {
                    dados.unlock();
                }
                System.out.println("# -> Terminando Esperar Posicao Vazia " + nome);
            }).start();
        }
    }

    /**
     * Redefine estado de infecao de determinado cliente
     * @param dados Dados
     * @param bytes mensagem
     * @param c TaggedConnection
     */
    public static void setInfectadoTrue(Dados dados,byte[] bytes,TaggedConnection c) {
        String s = new String(bytes);
        dados.setInfectado(s,true);
        try {
            c.send(6," -> Adeus! Desejamos as melhoras!".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifica quantas pessoas estao em determinada posicao
     * @param dados Dados
     * @param bytes mensagem
     * @param c TaggedConnection
     */
    public static void peopleInPosition(Dados dados,byte[] bytes,TaggedConnection c){
        String s = new String(bytes);
        String[] temp = s.split(",",2);
        int x = Integer.parseInt(temp[0]);
        int y = Integer.parseInt(temp[1]);
        int total;
        total = dados.peopleInPosition(x,y);
        try {
            c.send(7, ("Existem na posição ("+x+","+y+") " + total + " Pessoas").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Confirma rececao de aviso de determinado cliente
     * @param dados Dados
     * @param bytes mensagem
     * @param c TaggedConnection
     */
    public static void confirmaAviso(Dados dados,byte[] bytes,TaggedConnection c){
        Cliente cli;
        try {
            dados.lock();
            cli = dados.getCliente(new String(bytes));
            cli.lock();
        } finally {
            dados.unlock();
        }
        try {
            cli.setAviso(false);
        } finally {
            cli.unlock();
        }
        try {
            c.send(11, ("Aviso resetado!").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Mostra mapa existente
     * @param dados Dados
     * @param bytes mensagem
     * @param c TaggedConnection
     */
    public static void mapa(Dados dados,byte[] bytes,TaggedConnection c){
        System.out.println("# -> Mostrando mapa: ");
        String res;
        Cliente cli;
        boolean moderador;
        try {
            dados.lock();
            cli = dados.getCliente(new String(bytes));
            cli.lock();
        } finally {
            dados.unlock();
        }
        try {
            moderador = cli.getModerador();
        } finally {
            cli.unlock();
        }
        if (moderador) {
            res = dados.mapaModerador();
        } else res = "Não tens autorização suficiente para ver o Mapa\n";
        try {
            c.send(9,res.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


/* Recebe
 *  flag 0 -> logOut
 *  flag 1 -> int autenticar(String nome, String pass)
 *  flag 2 -> boolean registar(String nome, String pass)
 *  flag 3 -> boolean registarModerador (String nome,String pass)
 *  flag 4 -> void addPosicao(String nome, Posicao posicao)
 *  flag 5 -> int setInfectadoTrue(String cliente)
 *  flag 6 -> int numeroPessoasPosicao(Posicao posicao)
 *  flag 7 -> public String mapaModerador()
 *  flag 8 -> public boolean confirmarAviso()*/

/*Envia
flag -1 = LogOut
  flag 0 = Username Taken
  flag 1 = Registado com sucesso
  flag 2 = Esta Infetado
  flag 3 = Username Não Encontrado
  flag 4 = Sucesso Proseguir
  flag 5 = Pass Errada
  flag 12 = Já Online.
flag 6 = Registo Infetado Sucesso
flag 7 = Numero de Pessoal Em Posição
flag 8 = Aviso de possivel Infeção
flag 9 = Mapa de Morador
flag 10= Posicao registrada com sucesso
flag 11= AvisoConfirmado
*/