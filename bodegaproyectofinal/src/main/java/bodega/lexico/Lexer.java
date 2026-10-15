package bodega.lexico;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private static final List<String> ACC = List.of("REGISTRAR", "ASIGNAR", "ALMACENAR",
            "AUTORIZAR_RETIRO", "RETIRAR", "APAGAR_ALARMA", "CONSULTAR");
    private static final List<String> TIPOS = List.of("FRAGIL", "NORMAL");
    private static final List<String> TAMS = List.of("GRANDE", "MEDIANO", "PEQUEÑO");

    public List<Token> analizar(String txt) {
        List<Token> lista = new ArrayList<>();
        int i = 0;
        while (i < txt.length()) {
            char c = txt.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '(') {
                lista.add(new Token(TipoToken.PAR_IZQ, "("));
                i++;
            } else if (c == ')') {
                lista.add(new Token(TipoToken.PAR_DER, ")"));
                i++;
            } else if (c == ',') {
                lista.add(new Token(TipoToken.COMA, ","));
                i++;
            } else if (Character.isLetterOrDigit(c)) {
                int ini = i;
                while (i < txt.length() && (Character.isLetterOrDigit(txt.charAt(i)) || txt.charAt(i) == '_')) {
                    i++;
                }
                String pal = txt.substring(ini, i);
                if (ACC.contains(pal)) {
                    lista.add(new Token(TipoToken.ACCION, pal));
                } else if (TIPOS.contains(pal)) {
                    lista.add(new Token(TipoToken.TIPO, pal));
                } else if (TAMS.contains(pal)) {
                    lista.add(new Token(TipoToken.TAMANO, pal));
                } else if (pal.matches("[A-Za-z][A-Za-z0-9]*")) {
                    lista.add(new Token(TipoToken.ID, pal));
                } else {
                    lista.add(new Token(TipoToken.ERROR, pal));
                }
            } else {
                lista.add(new Token(TipoToken.ERROR, String.valueOf(c)));
                i++;
            }
        }
        return lista;
    }
}