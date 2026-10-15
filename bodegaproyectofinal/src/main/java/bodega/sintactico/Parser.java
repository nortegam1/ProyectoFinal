package bodega.sintactico;

import bodega.lexico.TipoToken;
import bodega.lexico.Token;
import java.util.ArrayList;
import java.util.List;

public class Parser {

    private List<Token> toks;
    private int pos;

    public List<Comando> analizar(List<Token> t) throws Exception {
        toks = t;
        pos = 0;
        List<Comando> cmds = new ArrayList<>();
        if (toks.isEmpty()) {
            throw new Exception("No hay comandos");
        }
        while (pos < toks.size()) {
            cmds.add(comando());
        }
        return cmds;
    }

    private Token esperar(TipoToken tipo) throws Exception {
        if (pos >= toks.size()) {
            throw new Exception("Se esperaba " + tipo + " pero el texto termino");
        }
        Token t = toks.get(pos);
        if (t.tipo != tipo) {
            throw new Exception("Se esperaba " + tipo + " y se encontro '" + t.valor + "'");
        }
        pos++;
        return t;
    }

    private Comando comando() throws Exception {
        Comando c = new Comando();
        c.accion = esperar(TipoToken.ACCION).valor;
        esperar(TipoToken.PAR_IZQ);
        if (c.accion.equals("REGISTRAR")) {
            c.tipo = esperar(TipoToken.TIPO).valor;
            esperar(TipoToken.COMA);
            c.id = esperar(TipoToken.ID).valor;
            esperar(TipoToken.COMA);
            c.tam = esperar(TipoToken.TAMANO).valor;
        } else if (!c.accion.equals("CONSULTAR")) {
            c.id = esperar(TipoToken.ID).valor;
        }
        esperar(TipoToken.PAR_DER);
        return c;
    }
}