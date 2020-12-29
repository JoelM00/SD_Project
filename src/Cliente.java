import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) throws Exception {
        Socket s = new Socket("localhost", 12345);
        TaggedConnection t = new TaggedConnection(s);
        Scanner in = new Scanner(System.in);


        System.out.println("Quer autenticar(1) ou registar(0)?");
        int x = in.nextInt();

        in.nextLine();
        System.out.print("Nome: ");
        String nome = in.nextLine();

        System.out.print("Pass: ");
        String pass = in.nextLine();

        System.out.println();

        t.send(x,nome.concat(" ").concat(pass).getBytes());

        TaggedConnection.Frame f = t.receive();
        System.out.println(" -> "+f.tag+" "+new String(f.dados));

        boolean stop = false;

        do {
            System.out.print("Diga uma flag de 2 a 3: ");
            int res = in.nextInt();
            switch (res) {
                case 2 -> {
                    System.out.print("Diga o x: ");
                    x = in.nextInt();
                    System.out.print("Diga o y: ");
                    int y = in.nextInt();
                    t.send(2,(nome+" "+x+" "+y).getBytes());
                }
                case 3 -> {
                    t.send(3, nome.getBytes());
                    f = t.receive();
                    System.out.println(new String(f.dados));
                    stop = true;
                }

            }
        } while (in.nextLine()!=null && !stop);

        t.close();
    }
}
