package bodega.lexico;

public class Token {
    public TipoToken tipo;
    public String valor;

    public Token(TipoToken tipo, String valor) {
        this.tipo = tipo;
        this.valor = valor;
    }

    @Override
    public String toString() {
        return tipo + " : " + valor;
    }
}