package bodega.serial;

import com.fazecast.jSerialComm.SerialPort;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.function.Consumer;

public class Arduino {

    private SerialPort puerto;
    private PrintWriter out;

    public String[] puertos() {
        try {
            SerialPort[] ps = SerialPort.getCommPorts();
            String[] nombres = new String[ps.length];
            for (int i = 0; i < ps.length; i++) {
                nombres[i] = ps[i].getSystemPortName();
            }
            return nombres;
        } catch (Throwable e) {
            return new String[0];
        }
    }

    public boolean conectar(String nombre, Consumer<String> alRecibir) {
        try {
            puerto = SerialPort.getCommPort(nombre);
            puerto.setBaudRate(9600);
            puerto.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
            if (!puerto.openPort()) {
                return false;
            }
            out = new PrintWriter(puerto.getOutputStream(), true);
            Thread t = new Thread(() -> {
                try (Scanner sc = new Scanner(puerto.getInputStream())) {
                    while (sc.hasNextLine()) {
                        alRecibir.accept(sc.nextLine().trim());
                    }
                } catch (Exception e) {
                }
            });
            t.setDaemon(true);
            t.start();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public void enviar(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public boolean conectado() {
        return puerto != null && puerto.isOpen();
    }

    public void desconectar() {
        if (puerto != null) {
            puerto.closePort();
        }
        out = null;
    }
}