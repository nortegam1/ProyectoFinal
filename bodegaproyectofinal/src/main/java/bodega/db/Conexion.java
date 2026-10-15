package bodega.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class Conexion {

    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=MiumgProyectoFinal;encrypt=true;trustServerCertificate=true";
    private static final String USR = "bodega_user";
    private static final String PWD = "Bodega123";

    public static Connection conectar() throws Exception {
        return DriverManager.getConnection(URL, USR, PWD);
    }
}