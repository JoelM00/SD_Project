import java.util.Objects;

public class Posicao {
    /**
     * Variaveis de instancia
     */
    public int x;
    public int y;

    /**
     * Construtor parametrizado
     * @param x coordenada
     * @param y coordenada
     */
    public Posicao(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Retorna clone do objeto
     * @return Posicao
     */
    @Override
    public Posicao clone(){
        return new Posicao(this.x,this.y);
    }

    /**
     * Retorna string do objeto
     * @return String
     */
    @Override
    public String toString() {
        return "Posicao{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    /**
     * Comparador de objetos
     * @param o Object
     * @return boolean
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Posicao posicao = (Posicao) o;
        return x == posicao.x && y == posicao.y;
    }

    /**
     * Funcao de hash
     * @return int
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
