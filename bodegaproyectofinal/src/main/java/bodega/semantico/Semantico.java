package bodega.semantico;

import bodega.db.Conexion;
import bodega.sintactico.Comando;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Semantico {

    public void validar(Comando c) throws Exception {
        if (c.accion.equals("CONSULTAR")) {
            return;
        }
        try (Connection cn = Conexion.conectar()) {
            PreparedStatement ps = cn.prepareStatement("SELECT estado FROM PAQUETE WHERE id = ?");
            ps.setString(1, c.id);
            ResultSet rs = ps.executeQuery();
            String est = rs.next() ? rs.getString(1) : null;

            if (c.accion.equals("REGISTRAR")) {
                if (est != null && !est.equals("Retirado")) {
                     throw new Exception("El paquete " + c.id + " ya existe");

                }
                return;
            }
            if (est == null) {
                throw new Exception("El paquete " + c.id + " no existe");
            }

            String req = "";
            switch (c.accion) {
                case "ASIGNAR":
                    req = "Registrado";
                    break;
                case "ALMACENAR":
                    req = "Asignado";
                    break;
                case "AUTORIZAR_RETIRO":
                    req = "Almacenado";
                    break;
                case "RETIRAR":
                    req = "Autorizado";
                    break;
            }
            if (!req.isEmpty() && !est.equals(req)) {
                throw new Exception("El paquete " + c.id + " esta " + est + " y debe estar " + req);
            }

            if (c.accion.equals("APAGAR_ALARMA")) {
                ps = cn.prepareStatement("SELECT COUNT(*) FROM ALARMA WHERE id_paquete = ? AND estado = 'Activa'");
                ps.setString(1, c.id);
                rs = ps.executeQuery();
                rs.next();
                if (rs.getInt(1) == 0) {
                    throw new Exception("El paquete " + c.id + " no tiene alarma activa");
                }
            }
        }
    }
}