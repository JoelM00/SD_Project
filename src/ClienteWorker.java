
import java.util.List;
import java.util.Scanner;

public class ClienteWorker {
    public static void main(String[] args) throws Exception {
        Scanner in = new Scanner(System.in);
        Stub stub = new Stub();

        String Username = null;
        boolean autenticado = false;

        while (!autenticado) {
            System.out.println("LOGIN: [0] -> sair, [1] -> autenticar, [2] -> registar, [3] -> registarModerador");
            int opcao = in.nextInt();
            in.nextLine();
            if (opcao == 0) {
                System.out.println(" -> Ok, have a good time!");
                break;
            }
            if (1 <= opcao && opcao <= 3) {
                System.out.print("Nome: ");
                String nome = in.nextLine();
                Username = nome;
                System.out.print("Pass: ");
                String pass = in.nextLine();
                System.out.println();
                int res = stub.autenticarOrRegistrar(opcao,nome,pass);
                switch (res) {
                    case 0 -> System.out.println(" -> Username utilizado!");
                    case 1 -> System.out.println(" -> Registado com sucesso!");
                    case 2 -> System.out.println(" -> Infetado, tem acesso negado!");
                    case 3 -> System.out.println(" -> Username não encontrado!");
                    case 4 -> {
                        System.out.println(" -> Login com sucesso!");
                        autenticado = true;
                    }
                    case 5 -> System.out.println(" -> Palavra pass errada!");
                    case 12 -> System.out.println(" -> Sua conta esta online noutro dispositivo!");
                }
            } else System.out.println(" -> Comando inválido!");
        }

        if (autenticado) {
                boolean stop = false;
                do {
                    System.out.print(menu(stub));
                    String resultado;
                    int res = in.nextInt();
                    switch (res) {
                        case 0 -> {
                            resultado = stub.trataTerminaConexao(Username);
                            System.out.println(resultado);
                            stop = true;
                        }
                        case 1 -> {
                            System.out.print("Diga o X: ");
                            int x = in.nextInt();
                            System.out.print("Diga o Y: ");
                            int y = in.nextInt();
                            resultado = stub.trataAddPosicao(x, y, Username);
                            System.out.println(resultado);
                            in.nextLine();
                        }
                        case 2 -> {
                            resultado = stub.trataSetInfetado(Username);
                            System.out.println(resultado);
                            stop = true;
                        }
                        case 3 -> {
                            System.out.print("Digite X: ");
                            int x = in.nextInt();
                            System.out.print("Digite Y: ");
                            int y = in.nextInt();
                            resultado = stub.trataGetPessoas(x, y);
                            System.out.println(resultado);
                            in.nextLine();
                        }
                        case 4 -> {
                            resultado = stub.trataMapa(Username);
                            System.out.println(resultado);
                            in.nextLine();
                        }
                        case 5 -> {
                            resultado = stub.trataRecepcaoAviso(Username);
                            System.out.println(resultado);
                            in.nextLine();
                        }
                        case 6 -> {
                            List<String> mns = stub.verMns();
                            if (mns.size()==0)
                                System.out.println(" -> Você não tem mensagens!");
                            for (String a : mns) {
                                System.out.println(a);
                            }
                            in.nextLine();
                        }
                        case 7 -> {
                            stub.clearMns();
                            System.out.println(" -> Mensagens apagadas!");
                        }
                        case 8 -> {
                            System.out.print("Digite X: ");
                            int x = in.nextInt();
                            System.out.print("Digite Y: ");
                            int y = in.nextInt();
                            resultado = stub.trataEsperarPosicaoVazia(x, y,Username);
                            System.out.println(resultado);
                            in.nextLine();
                        }

                        default -> System.out.println(" -> Comando inválido");
                    }
                } while (in.nextLine() != null && !stop);
            System.out.println(" -> Adeus\n");
        }
    }

    /**
     * Menu principal
     * @param stub Representante do cliente
     * @return String
     */
    public static String menu(Stub stub) {
        return  "\n# # # # # # # # MENU # # # # # # # #\n"+
                " -> Você tem: " + stub.quantasMns() + " Mensagens!\n\n" +
                "[0] - Sair\n" +
                "[1] - Adicionar posição\n" +
                "[2] - Relatar infeção\n" +
                "[3] - Número de pessoas numa posição\n" +
                "[4] - Mapa de posições\n" +
                "[5] - Confimar recepção de aviso\n" +
                "[6] - Listar mensagens\n" +
                "[7] - Apagar mensagens\n" +
                "[8] - Esperar Por Posição Vazia\n\n" +
                "Opção: ";
    }
}