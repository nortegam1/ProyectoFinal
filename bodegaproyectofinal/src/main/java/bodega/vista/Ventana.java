package bodega.vista;

import bodega.acciones.Acciones;
import bodega.db.Conexion;
import bodega.lexico.Lexer;
import bodega.semantico.Semantico;
import bodega.serial.Arduino;
import bodega.sintactico.Comando;
import bodega.sintactico.Parser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class Ventana extends JFrame {

    private static final String[] ESP = {"E1", "E2", "E4"};

    private JTextField txtCmd = new JTextField();
    private JButton btnEj = new JButton("Ejecutar");
    private JComboBox<String> cbPaq = new JComboBox<>();
    private JButton btnSim = new JButton("Simular");
    private JLabel lblAl = new JLabel("Sin alarmas");
    private JComboBox<String> cbPto = new JComboBox<>();
    private JButton btnCon = new JButton("Conectar");
    private JLabel lblCon = new JLabel("Arduino desconectado");
    private JTextArea txtLog = new JTextArea();
    private DefaultTableModel mEsp = new DefaultTableModel(
            new String[]{"Espacio", "Tamaño", "Tipo", "Ocupación", "Estado"}, 0);
    private DefaultTableModel mPaq = new DefaultTableModel(
            new String[]{"Paquete", "Tipo", "Tamaño", "Espacio", "Estado"}, 0);
    private DefaultTableModel mAl = new DefaultTableModel(
            new String[]{"Paquete", "Espacio", "Fecha"}, 0);
    private Lexer lx = new Lexer();
    private Parser p = new Parser();
    private Semantico sem = new Semantico();
    private Acciones acc = new Acciones();
    private Arduino ard = new Arduino();

    public Ventana() {
        setTitle("Sistema Inteligente de Almacenamiento de Bodega");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 660);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        JPanel fila1 = new JPanel(new BorderLayout(5, 5));
        fila1.add(new JLabel("Comando:"), BorderLayout.WEST);
        fila1.add(txtCmd, BorderLayout.CENTER);
        fila1.add(btnEj, BorderLayout.EAST);

        JPanel sim = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        sim.add(new JLabel("Simular movimiento del paquete:"));
        sim.add(cbPaq);
        sim.add(btnSim);
        lblAl.setOpaque(true);
        lblAl.setHorizontalAlignment(JLabel.CENTER);
        lblAl.setPreferredSize(new Dimension(170, 24));
        JPanel fila2 = new JPanel(new BorderLayout(5, 5));
        fila2.add(sim, BorderLayout.WEST);
        fila2.add(lblAl, BorderLayout.EAST);

        cbPto.setEditable(true);
        for (String s : ard.puertos()) {
            cbPto.addItem(s);
        }
        JPanel fila3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        fila3.add(new JLabel("Puerto del Arduino:"));
        fila3.add(cbPto);
        fila3.add(btnCon);
        fila3.add(lblCon);

        JPanel norte = new JPanel(new GridLayout(3, 1, 5, 5));
        norte.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        norte.add(fila1);
        norte.add(fila2);
        norte.add(fila3);
        add(norte, BorderLayout.NORTH);

        JTable tEsp = new JTable(mEsp);
        tEsp.setDefaultEditor(Object.class, null);
        tEsp.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String est = t.getValueAt(r, 4).toString();
                String ocu = t.getValueAt(r, 3).toString();
                if (est.equals("Lleno")) {
                    comp.setBackground(new Color(255, 200, 200));
                } else if (!ocu.startsWith("0/")) {
                    comp.setBackground(new Color(255, 245, 190));
                } else {
                    comp.setBackground(new Color(200, 245, 200));
                }
                comp.setForeground(Color.BLACK);
                return comp;
            }
        });
        JTable tPaq = new JTable(mPaq);
        tPaq.setDefaultEditor(Object.class, null);
        JTable tAl = new JTable(mAl);
        tAl.setDefaultEditor(Object.class, null);

        JScrollPane sEsp = new JScrollPane(tEsp);
        sEsp.setBorder(BorderFactory.createTitledBorder("Espacios"));
        JScrollPane sPaq = new JScrollPane(tPaq);
        sPaq.setBorder(BorderFactory.createTitledBorder("Paquetes"));
        JScrollPane sAl = new JScrollPane(tAl);
        sAl.setBorder(BorderFactory.createTitledBorder("Alarmas activas"));
        JPanel der = new JPanel(new GridLayout(2, 1, 5, 5));
        der.add(sPaq);
        der.add(sAl);
        JPanel centro = new JPanel(new GridLayout(1, 2, 5, 5));
        centro.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        centro.add(sEsp);
        centro.add(der);
        add(centro, BorderLayout.CENTER);

        txtLog.setEditable(false);
        txtLog.setText("Ejemplo: REGISTRAR(NORMAL,P1,MEDIANO)\n");
        JScrollPane sLog = new JScrollPane(txtLog);
        sLog.setBorder(BorderFactory.createTitledBorder("Mensajes"));
        sLog.setPreferredSize(new Dimension(0, 160));
        add(sLog, BorderLayout.SOUTH);

        btnEj.addActionListener(e -> ejecutar());
        txtCmd.addActionListener(e -> ejecutar());
        btnSim.addActionListener(e -> simular());
        btnCon.addActionListener(e -> conectar());
        actualizar();
    }

    private void ejecutar() {
        String txt = txtCmd.getText().trim();
        if (txt.isEmpty()) {
            return;
        }
        txtLog.append("> " + txt + "\n");
        try {
            for (Comando c : p.analizar(lx.analizar(txt))) {
                sem.validar(c);
                txtLog.append(acc.ejecutar(c) + "\n");
            }
        } catch (Exception e) {
            txtLog.append("Error: " + e.getMessage() + "\n");
        }
        txtCmd.setText("");
        actualizar();
    }

    private void simular() {
        String id = (String) cbPaq.getSelectedItem();
        if (id == null) {
            return;
        }
        txtLog.append("> Movimiento del paquete " + id + "\n");
        try {
            txtLog.append(acc.movimientoPaquete(id) + "\n");
        } catch (Exception e) {
            txtLog.append("Error: " + e.getMessage() + "\n");
        }
        actualizar();
    }

    private void conectar() {
        if (ard.conectado()) {
            ard.desconectar();
            btnCon.setText("Conectar");
            lblCon.setText("Arduino desconectado");
            return;
        }
        String pto = (String) cbPto.getSelectedItem();
        if (pto == null || pto.trim().isEmpty()) {
            txtLog.append("Elige el puerto del Arduino\n");
            return;
        }
        if (ard.conectar(pto.trim(), this::recibir)) {
            btnCon.setText("Desconectar");
            lblCon.setText("Conectado a " + pto.trim());
            javax.swing.Timer t = new javax.swing.Timer(2500, e -> actualizar());
            t.setRepeats(false);
            t.start();
        } else {
            txtLog.append("No se pudo abrir " + pto + "\n");
        }
    }

    private void recibir(String linea) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (linea.startsWith("MOV:")) {
                    String esp = ESP[Integer.parseInt(linea.substring(4).trim()) - 1];
                    txtLog.append("> Sensor: movimiento en el espacio " + esp + "\n");
                    txtLog.append(acc.movimiento(esp) + "\n");
                    actualizar();
                } else if (linea.equals("LISTO")) {
                    actualizar();
                } else if (linea.equals("BTN")) {
                    txtLog.append("Boton del circuito presionado\n");
                }
            } catch (Exception e) {
                txtLog.append("Error: " + e.getMessage() + "\n");
            }
        });
    }

    private void actualizar() {
        try (Connection cn = Conexion.conectar()) {
            mEsp.setRowCount(0);
            ResultSet rs = cn.createStatement().executeQuery(
                    "SELECT id_espacio, tamano, tipo_especial, "
                    + "CAST(ocupacion_actual AS VARCHAR) + '/' + CAST(capacidad_max AS VARCHAR), estado "
                    + "FROM ESPACIO ORDER BY id_espacio");
            while (rs.next()) {
                mEsp.addRow(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5)});
            }
            String sel = (String) cbPaq.getSelectedItem();
            cbPaq.removeAllItems();
            mPaq.setRowCount(0);
            rs = cn.createStatement().executeQuery(
                    "SELECT p.id, t.paquete_tipo, m.paquete_tamano, ISNULL(p.id_espacio, 'Sin asignar'), p.estado "
                    + "FROM PAQUETE p JOIN PAQUETE_TIPO t ON t.id_paquete_tipo = p.id_paquete_tipo "
                    + "JOIN PAQUETE_TAMANO m ON m.id_paquete_tamano = p.id_paquete_tamano "
                    + "WHERE p.estado <> 'Retirado' ORDER BY p.fecha_registro");
            while (rs.next()) {
                mPaq.addRow(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5)});
                if (rs.getString(5).equals("Almacenado") || rs.getString(5).equals("Autorizado")) {
                    cbPaq.addItem(rs.getString(1));
                }
            }
            if (sel != null) {
                cbPaq.setSelectedItem(sel);
            }
            mAl.setRowCount(0);
            rs = cn.createStatement().executeQuery(
                    "SELECT id_paquete, id_espacio, CONVERT(VARCHAR(19), fecha_activacion, 120) "
                    + "FROM ALARMA WHERE estado = 'Activa' ORDER BY fecha_activacion");
            while (rs.next()) {
                mAl.addRow(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3)});
            }
            if (mAl.getRowCount() > 0) {
                lblAl.setText("¡ALARMA ACTIVA!");
                lblAl.setBackground(Color.RED);
                lblAl.setForeground(Color.WHITE);
            } else {
                lblAl.setText("Sin alarmas");
                lblAl.setBackground(new Color(200, 245, 200));
                lblAl.setForeground(Color.BLACK);
            }
            if (ard.conectado()) {
                for (int i = 0; i < ESP.length; i++) {
                    ard.enviar("ESTADO:" + (i + 1) + ":" + acc.estadoEsp(ESP[i]));
                }
                ard.enviar("ALARMA:" + (mAl.getRowCount() > 0 ? 1 : 0));
            }
        } catch (Exception e) {
            txtLog.append("Error: " + e.getMessage() + "\n");
        }
    }
}