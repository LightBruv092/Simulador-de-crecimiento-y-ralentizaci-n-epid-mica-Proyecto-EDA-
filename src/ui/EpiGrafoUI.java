package ui;

import io.CargadorGrafo;
import modelo.Conexion;
import modelo.GrafoEpidemia;
import modelo.Localidad;
import optimizador.EstrategiaCorte;
import optimizador.ModeloEconomico;
import optimizador.SolucionadorContencion;
import simulacion.MotorEpidemia;
import simulacion.ModeloPropagacion;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * INTERFAZ VISUAL con Java Swing.
 *
 * Color del nodo según fracción infectada:
 *   Azul (0%)  →  Amarillo (50%)  →  Rojo (100%)
 *
 * Debajo de cada nodo: "X / Y infectados"
 */
public class EpiGrafoUI extends JFrame {

    private GrafoEpidemia    grafo;
    private ModeloPropagacion modeloProp;
    private MotorEpidemia    motor;

    private final Map<Integer, Point> posiciones = new HashMap<>();

    private Timer      timerSimulacion;
    private PanelGrafo panelGrafo;
    private JTextArea  areaInfo;
    private JLabel     labelTick;
    private JSlider    sliderPresupuesto;

    public EpiGrafoUI() {
        super("EpiGrafo — Simulador de Epidemias");
        inicializarModelo();
        inicializarUI();
        calcularPosicionesNodos();
        actualizarInfo();
    }

    private void inicializarModelo() {
        grafo      = CargadorGrafo.cargarEjemplo();
        modeloProp = new ModeloPropagacion();
        motor      = new MotorEpidemia(grafo, modeloProp);
    }

    private void inicializarUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(new Color(20, 25, 35));

        panelGrafo = new PanelGrafo();
        add(panelGrafo, BorderLayout.CENTER);

        JPanel panelDerecho = new JPanel(new BorderLayout(0, 8));
        panelDerecho.setBackground(new Color(30, 35, 45));
        panelDerecho.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));
        panelDerecho.setPreferredSize(new Dimension(270, 0));
        panelDerecho.add(crearPanelControles(), BorderLayout.NORTH);
        panelDerecho.add(crearPanelInfo(),      BorderLayout.CENTER);
        add(panelDerecho, BorderLayout.EAST);

        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        barraTop.setBackground(new Color(15, 20, 30));
        labelTick = new JLabel("Tick: 0");
        labelTick.setForeground(Color.WHITE);
        labelTick.setFont(new Font("Monospaced", Font.BOLD, 14));
        barraTop.add(labelTick);
        add(barraTop, BorderLayout.NORTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel crearPanelControles() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(30, 35, 45));
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 80, 120)),
                "Controles", 0, 0, null, Color.LIGHT_GRAY));

        JButton btnTick   = boton("▶ Un tick",          new Color(40, 100, 160));
        JButton btnAuto   = boton("⏩ Auto",             new Color(40, 130, 80));
        JButton btnCortar = boton("✂ Optimizar cortes", new Color(160, 50, 50));
        JButton btnReset  = boton("↺ Reiniciar",        new Color(80, 80, 80));

        btnTick.addActionListener(e -> avanzarTick());
        btnAuto.addActionListener(e -> toggleAnimacion(btnAuto));
        btnCortar.addActionListener(e -> optimizarCortes());
        btnReset.addActionListener(e -> reiniciar());

        sliderPresupuesto = crearSlider("Presupuesto $", 50, 500, 200);

        p.add(Box.createVerticalStrut(4));
        p.add(btnTick);   p.add(Box.createVerticalStrut(4));
        p.add(btnAuto);   p.add(Box.createVerticalStrut(4));
        p.add(btnCortar); p.add(Box.createVerticalStrut(4));
        p.add(btnReset);  p.add(Box.createVerticalStrut(8));
        p.add(sliderPresupuesto.getParent());
        return p;
    }

    private JPanel crearPanelInfo() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(25, 30, 40));
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 80, 120)),
                "Estado", 0, 0, null, Color.LIGHT_GRAY));
        areaInfo = new JTextArea();
        areaInfo.setEditable(false);
        areaInfo.setBackground(new Color(20, 25, 35));
        areaInfo.setForeground(new Color(180, 220, 180));
        areaInfo.setFont(new Font("Monospaced", Font.PLAIN, 12));
        p.add(new JScrollPane(areaInfo), BorderLayout.CENTER);
        return p;
    }

    // --- Acciones ---

    private void avanzarTick() {
        if (motor.todasLlenas()) {
            areaInfo.setText("Población completamente infectada.\nReinicia para empezar.");
            return;
        }
        motor.tick();
        labelTick.setText("Tick: " + motor.getTickActual());
        actualizarInfo();
        panelGrafo.repaint();
    }

    private void toggleAnimacion(JButton btn) {
        if (timerSimulacion != null && timerSimulacion.isRunning()) {
            timerSimulacion.stop();
            btn.setText("⏩ Auto");
        } else {
            timerSimulacion = new Timer(500, e -> {
                if (motor.todasLlenas()) { timerSimulacion.stop(); btn.setText("⏩ Auto"); }
                else avanzarTick();
            });
            timerSimulacion.start();
            btn.setText("⏸ Pausar");
        }
    }

    private void optimizarCortes() {
        if (timerSimulacion != null) timerSimulacion.stop();
        SolucionadorContencion s = new SolucionadorContencion(
                grafo, new ModeloEconomico(1.0), sliderPresupuesto.getValue());
        List<EstrategiaCorte> cortes = s.resolver();

        StringBuilder sb = new StringBuilder("=== CORTES APLICADOS ===\n\n");
        if (cortes.isEmpty()) sb.append("Ninguno (sin presupuesto\no todos en MST)");
        else cortes.forEach(c -> sb.append(c).append("\n"));
        areaInfo.setText(sb.toString());
        panelGrafo.repaint();
    }

    private void reiniciar() {
        if (timerSimulacion != null) timerSimulacion.stop();
        inicializarModelo();
        calcularPosicionesNodos();
        labelTick.setText("Tick: 0");
        actualizarInfo();
        panelGrafo.repaint();
    }

    private void actualizarInfo() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < grafo.getNumLocalidades(); i++) {
            Localidad l = grafo.getLocalidad(i);
            int pct = (int)(l.fraccionInfectada() * 100);
            String barra = barraProgreso(pct);
            sb.append(String.format("%-13s%n%s %d%%%n%d / %d%n%n",
                    l.nombre, barra, pct, l.infectados, l.poblacion));
        }
        areaInfo.setText(sb.toString());
    }

    private String barraProgreso(int pct) {
        int llenos = pct / 10;
        return "[" + "█".repeat(llenos) + "░".repeat(10 - llenos) + "]";
    }

    // --- Dibujo ---

    private void calcularPosicionesNodos() {
        int n = grafo.getNumLocalidades();
        int cx = 330, cy = 290, radio = 200;
        for (int i = 0; i < n; i++) {
            double ang = 2 * Math.PI * i / n - Math.PI / 2;
            posiciones.put(i, new Point(
                    (int)(cx + radio * Math.cos(ang)),
                    (int)(cy + radio * Math.sin(ang))));
        }
    }

    class PanelGrafo extends JPanel {

        PanelGrafo() { setBackground(new Color(15, 20, 30)); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            dibujarAristas(g2);
            dibujarNodos(g2);
            dibujarLeyenda(g2);
        }

        private void dibujarAristas(Graphics2D g2) {
            for (int u = 0; u < grafo.getNumLocalidades(); u++) {
                for (Conexion c : grafo.getAdj(u)) {
                    if (u >= c.destino) continue;
                    Point p1 = posiciones.get(u);
                    Point p2 = posiciones.get(c.destino);

                    if (c.cortada) {
                        g2.setColor(new Color(180, 50, 50, 160));
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER, 10, new float[]{7, 7}, 0));
                    } else {
                        float grosor = (float)(c.tasaMovilidad * 5 + 1);
                        g2.setColor(new Color(90, 130, 200, 180));
                        g2.setStroke(new BasicStroke(grosor));
                    }
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);

                    // Peso en el centro
                    int mx = (p1.x + p2.x) / 2;
                    int my = (p1.y + p2.y) / 2;
                    g2.setColor(new Color(160, 160, 160));
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
                    g2.drawString(String.valueOf(c.peso), mx + 4, my - 4);
                }
            }
        }

        private void dibujarNodos(Graphics2D g2) {
            final int R = 34;
            for (int i = 0; i < grafo.getNumLocalidades(); i++) {
                Localidad l = grafo.getLocalidad(i);
                Point p     = posiciones.get(i);
                Color fill  = colorPorFraccion(l.fraccionInfectada());

                // Sombra
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillOval(p.x - R + 3, p.y - R + 3, R * 2, R * 2);

                // Relleno
                g2.setColor(fill);
                g2.fillOval(p.x - R, p.y - R, R * 2, R * 2);

                // Borde
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(p.x - R, p.y - R, R * 2, R * 2);

                // Nombre
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String nombre = l.nombre.split(" ")[0];
                g2.drawString(nombre, p.x - fm.stringWidth(nombre) / 2, p.y);

                // Porcentaje dentro del nodo
                String pct = String.format("%.0f%%", l.fraccionInfectada() * 100);
                g2.setFont(new Font("Monospaced", Font.BOLD, 11));
                fm = g2.getFontMetrics();
                g2.setColor(new Color(255, 230, 150));
                g2.drawString(pct, p.x - fm.stringWidth(pct) / 2, p.y + 15);

                // Infectados / total debajo del círculo
                String sub = l.infectados + "/" + l.poblacion;
                g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                fm = g2.getFontMetrics();
                g2.setColor(new Color(190, 190, 190));
                g2.drawString(sub, p.x - fm.stringWidth(sub) / 2, p.y + R + 15);
            }
        }

        /**
         * Interpola azul → amarillo → rojo según fracción 0.0–1.0
         */
        private Color colorPorFraccion(double f) {
            f = Math.max(0, Math.min(1, f));
            if (f < 0.5) {
                // azul → amarillo
                int r = (int)(f * 2 * 200);
                int g = (int)(f * 2 * 160 + 60);
                int b = (int)((1 - f * 2) * 160 + 20);
                return new Color(r, g, b);
            } else {
                // amarillo → rojo
                double t = (f - 0.5) * 2;
                int r = 200;
                int g = (int)((1 - t) * 160);
                int b = 20;
                return new Color(r, g, b);
            }
        }

        private void dibujarLeyenda(Graphics2D g2) {
            int x = 12, y = 20;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));

            // Gradiente de leyenda
            for (int i = 0; i <= 4; i++) {
                double f = i / 4.0;
                g2.setColor(colorPorFraccion(f));
                g2.fillRect(x + i * 28, y, 28, 12);
            }
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(x, y, 140, 12);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("0%", x, y + 26);
            g2.drawString("50%", x + 57, y + 26);
            g2.drawString("100%", x + 108, y + 26);
            g2.drawString("infectados", x + 20, y + 40);
        }
    }

    // --- Helpers ---

    private JButton boton(String texto, Color color) {
        JButton b = new JButton(texto);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return b;
    }

    private JSlider crearSlider(String etiqueta, int min, int max, int valor) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(30, 35, 45));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        JLabel lbl = new JLabel(etiqueta + ": " + valor);
        lbl.setForeground(Color.LIGHT_GRAY);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JSlider sl = new JSlider(min, max, valor);
        sl.setBackground(new Color(30, 35, 45));
        sl.addChangeListener(e -> lbl.setText(etiqueta + ": " + sl.getValue()));
        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(sl,  BorderLayout.CENTER);
        return sl;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(EpiGrafoUI::new);
    }
}
