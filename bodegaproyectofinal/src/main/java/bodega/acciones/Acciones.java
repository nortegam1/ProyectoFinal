package bodega.acciones;

import bodega.db.Conexion;
import bodega.sintactico.Comando;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Acciones {

    public String ejecutar(Comando c) throws Exception {
        try (Connection cn = Conexion.conectar()) {
            cn.setAutoCommit(false);
            try {
                String res = "";
                switch (c.accion) {
                    case "REGISTRAR":
                        res = registrar(cn, c);
                        break;
                    case "ASIGNAR":
                        res = asignar(cn, c);
                        break;
                    case "ALMACENAR":
                        res = almacenar(cn, c);
                        break;
                    case "AUTORIZAR_RETIRO":
                        res = autorizar(cn, c);
                        break;
                    case "RETIRAR":
                        res = retirar(cn, c);
                        break;
                    case "APAGAR_ALARMA":
                        res = apagar(cn, c);
                        break;
                    case "CONSULTAR":
                        res = consultar(cn);
                        break;
                }
                cn.commit();
                return res;
            } catch (Exception e) {
                cn.rollback();
                throw e;
            }
        }
    }

    public String movimiento(String esp) throws Exception {
        try (Connection cn = Conexion.conectar()) {
            PreparedStatement ps = cn.prepareStatement(
                    "SELECT TOP 1 id FROM PAQUETE WHERE id_espacio = ? AND estado = 'Almacenado' "
                    + "AND id NOT IN (SELECT id_paquete FROM ALARMA WHERE estado = 'Activa') "
                    + "ORDER BY fecha_registro");
            ps.setString(1, esp);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return "Movimiento en " + esp + ": sin alarma (no hay paquete almacenado sin autorizar en este espacio)";
            }
            String id = rs.getString(1);
            upd(cn, "INSERT INTO ALARMA (id_paquete, id_espacio) VALUES (?, ?)", id, esp);
            hist(cn, id, esp, "ALARMA");
            return "ALARMA: movimiento no autorizado del paquete " + id + " en el espacio " + esp;
        }
    }

    public String movimientoPaquete(String id) throws Exception {
        try (Connection cn = Conexion.conectar()) {
            PreparedStatement ps = cn.prepareStatement(
                    "SELECT id_espacio FROM PAQUETE WHERE id = ? AND estado = 'Almacenado' "
                    + "AND id NOT IN (SELECT id_paquete FROM ALARMA WHERE estado = 'Activa')");
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return "Movimiento del paquete " + id + ": sin alarma (esta autorizado, ya tiene alarma o no esta almacenado)";
            }
            String esp = rs.getString(1);
            upd(cn, "INSERT INTO ALARMA (id_paquete, id_espacio) VALUES (?, ?)", id, esp);
            hist(cn, id, esp, "ALARMA");
            return "ALARMA: movimiento no autorizado del paquete " + id + " en el espacio " + esp;
        }
    }

    public String estadoEsp(String esp) throws Exception {
        try (Connection cn = Conexion.conectar()) {
            PreparedStatement ps = cn.prepareStatement(
                    "SELECT DISTINCT estado FROM PAQUETE WHERE id_espacio = ? "
                    + "AND estado IN ('Asignado','Almacenado','Autorizado')");
            ps.setString(1, esp);
            ResultSet rs = ps.executeQuery();
            boolean asig = false, alm = false, aut = false;
            while (rs.next()) {
                String e = rs.getString(1);
                if (e.equals("Asignado")) {
                    asig = true;
                } else if (e.equals("Almacenado")) {
                    alm = true;
                } else if (e.equals("Autorizado")) {
                    aut = true;
                }
            }
            if (alm) {
                return "A";
            }
            if (aut) {
                return "U";
            }
            return asig ? "O" : "L";
        }
    }

    private String registrar(Connection cn, Comando c) throws Exception {
        PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM PAQUETE WHERE id = ?");
        ps.setString(1, c.id);
        ResultSet rs = ps.executeQuery();
        rs.next();
        if (rs.getInt(1) > 0) {
            upd(cn, "UPDATE PAQUETE SET id_espacio = NULL, estado = 'Registrado', fecha_registro = GETDATE(), "
                    + "id_paquete_tipo = (SELECT id_paquete_tipo FROM PAQUETE_TIPO WHERE paquete_tipo = ?), "
                    + "id_paquete_tamano = (SELECT id_paquete_tamano FROM PAQUETE_TAMANO WHERE paquete_tamano = ?) "
                    + "WHERE id = ?", cap(c.tipo), cap(c.tam), c.id);
            upd(cn, "UPDATE ALARMA SET estado = 'Apagada' WHERE id_paquete = ? AND estado = 'Activa'", c.id);
        } else {
            upd(cn, "INSERT INTO PAQUETE (id, id_paquete_tipo, id_paquete_tamano) VALUES (?, "
                    + "(SELECT id_paquete_tipo FROM PAQUETE_TIPO WHERE paquete_tipo = ?), "
                    + "(SELECT id_paquete_tamano FROM PAQUETE_TAMANO WHERE paquete_tamano = ?))",
                    c.id, cap(c.tipo), cap(c.tam));
        }
        hist(cn, c.id, null, c.accion);
        return "Paquete " + c.id + " registrado";
    }

    private String asignar(Connection cn, Comando c) throws Exception {
        PreparedStatement ps = cn.prepareStatement(
                "SELECT TOP 1 e.id_espacio FROM ESPACIO e, PAQUETE p, PAQUETE_TIPO t, PAQUETE_TAMANO m "
                + "WHERE p.id = ? AND t.id_paquete_tipo = p.id_paquete_tipo "
                + "AND m.id_paquete_tamano = p.id_paquete_tamano "
                + "AND e.tipo_especial = t.paquete_tipo AND e.tamano = m.paquete_tamano "
                + "AND e.ocupacion_actual < e.capacidad_max ORDER BY e.id_espacio");
        ps.setString(1, c.id);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new Exception("No hay espacio disponible para " + c.id);
        }
        String esp = rs.getString(1);
        upd(cn, "UPDATE PAQUETE SET id_espacio = ?, estado = 'Asignado' WHERE id = ?", esp, c.id);
        upd(cn, "UPDATE ESPACIO SET ocupacion_actual = ocupacion_actual + 1, "
                + "estado = CASE WHEN ocupacion_actual + 1 >= capacidad_max THEN 'Lleno' ELSE 'Disponible' END "
                + "WHERE id_espacio = ?", esp);
        hist(cn, c.id, esp, c.accion);
        return "Paquete " + c.id + " asignado al espacio " + esp;
    }

    private String almacenar(Connection cn, Comando c) throws Exception {
        upd(cn, "UPDATE PAQUETE SET estado = 'Almacenado' WHERE id = ?", c.id);
        String esp = esp(cn, c.id);
        hist(cn, c.id, esp, c.accion);
        return "Paquete " + c.id + " almacenado en el espacio " + esp;
    }

    private String autorizar(Connection cn, Comando c) throws Exception {
        upd(cn, "INSERT INTO AUTORIZACION_RETIRO (id_paquete) VALUES (?)", c.id);
        upd(cn, "UPDATE PAQUETE SET estado = 'Autorizado' WHERE id = ?", c.id);
        hist(cn, c.id, esp(cn, c.id), c.accion);
        return "Retiro del paquete " + c.id + " autorizado";
    }

    private String retirar(Connection cn, Comando c) throws Exception {
        String esp = esp(cn, c.id);
        upd(cn, "UPDATE PAQUETE SET estado = 'Retirado' WHERE id = ?", c.id);
        upd(cn, "UPDATE ESPACIO SET ocupacion_actual = ocupacion_actual - 1, estado = 'Disponible' "
                + "WHERE id_espacio = ?", esp);
        upd(cn, "UPDATE AUTORIZACION_RETIRO SET estado = 'Utilizado' WHERE id_paquete = ? AND estado = 'Autorizado'", c.id);
        hist(cn, c.id, esp, c.accion);
        return "Paquete " + c.id + " retirado, espacio " + esp + " liberado";
    }

    private String apagar(Connection cn, Comando c) throws Exception {
        upd(cn, "UPDATE ALARMA SET estado = 'Apagada' WHERE id_paquete = ? AND estado = 'Activa'", c.id);
        hist(cn, c.id, esp(cn, c.id), c.accion);
        return "Alarma del paquete " + c.id + " apagada";
    }

    private String consultar(Connection cn) throws Exception {
        ResultSet rs = cn.createStatement().executeQuery(
                "SELECT id_espacio, tamano, tipo_especial, ocupacion_actual, capacidad_max, estado "
                + "FROM ESPACIO ORDER BY id_espacio");
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            sb.append(rs.getString(1)).append(" | ").append(rs.getString(2)).append(" | ")
                    .append(rs.getString(3)).append(" | ").append(rs.getInt(4)).append("/")
                    .append(rs.getInt(5)).append(" | ").append(rs.getString(6)).append("\n");
        }
        return sb.toString();
    }

    private void upd(Connection cn, String sql, String... p) throws Exception {
        PreparedStatement ps = cn.prepareStatement(sql);
        for (int i = 0; i < p.length; i++) {
            ps.setString(i + 1, p[i]);
        }
        ps.executeUpdate();
    }

    private void hist(Connection cn, String id, String esp, String acc) throws Exception {
        upd(cn, "INSERT INTO HISTORIAL_MOV (id_paquete, id_espacio, accion_realizada) VALUES (?, ?, ?)", id, esp, acc);
    }

    private String esp(Connection cn, String id) throws Exception {
        PreparedStatement ps = cn.prepareStatement("SELECT id_espacio FROM PAQUETE WHERE id = ?");
        ps.setString(1, id);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getString(1);
    }

    private String cap(String t) {
        return t.charAt(0) + t.substring(1).toLowerCase();
    }
}