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
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

// Interfaz gráfica principal (Swing)
public class EpiGrafoUI extends JFrame {

    private GrafoEpidemia     grafo;
    private GrafoEpidemia     snapshotInicial;
    private ModeloPropagacion modeloProp;
    private MotorEpidemia     motor;

    private final Map<Integer, Point> posiciones = new HashMap<>();

    private Timer      timerSimulacion;
    private PanelGrafo panelGrafo;
    private JTextArea  areaInfo;
    private JLabel     labelTick;
    private JSlider    sliderPresupuesto;
    private int        nodoSeleccionado = -1;
    private final Map<String, BufferedImage> cacheImagenes = new HashMap<>();
    private BufferedImage imagenDefault;

    private BufferedImage cargarImagenCiudad(String nombreCiudad) {
        try {
            // "Limpia" el texto
            String limpio = nombreCiudad.replaceAll("\\s+", "");
            String nombreArchivo =
            limpio.substring(0,1).toUpperCase()
            + limpio.substring(1).toLowerCase()
            + ".png";
            
            // ===== CACHE =====
            if (cacheImagenes.containsKey(nombreArchivo)) {
                return cacheImagenes.get(nombreArchivo);
            }

            // ===== BUSCAR IMAGEN =====
            System.out.println("BUSCANDO: " + nombreArchivo);
            java.net.URL url =
                    getClass().getResource("/mapas/" + nombreArchivo);
            BufferedImage img;
            
            // Si no existe usa default
            if (url == null) {
                System.out.println("NO ENCONTRADA: " + nombreArchivo);
                java.net.URL defaultUrl =
                        getClass().getResource("/mapas/Default.png");

                if (defaultUrl == null) {
                    System.out.println("TAMPOCO EXISTE Default.png");
                    return null;
                }
                img = ImageIO.read(defaultUrl);

            } else {
                System.out.println("CARGADA: " + nombreArchivo);
                img = ImageIO.read(url);
            }
            cacheImagenes.put(nombreArchivo, img);
            return img;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    

    
    public EpiGrafoUI() {
        super("EpiGrafo — Simulador de Epidemias");
        inicializarModelo();
        inicializarUI();
        calcularPosicionesNodos();
        actualizarInfo();
    }

    // --- Inicialización ---

    private void inicializarModelo() {
        grafo           = CargadorGrafo.cargarVacio();
        snapshotInicial = grafo.clonarEstructura();
        modeloProp      = new ModeloPropagacion();
        motor           = new MotorEpidemia(grafo, modeloProp);
    }

    private void inicializarUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 680);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(new Color(20, 25, 35));

        panelGrafo = new PanelGrafo();
        add(panelGrafo, BorderLayout.CENTER);

        JPanel panelDerecho = new JPanel(new BorderLayout(0, 8));
        panelDerecho.setBackground(new Color(30, 35, 45));
        panelDerecho.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));
        panelDerecho.setPreferredSize(new Dimension(285, 0));
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
        
        SwingUtilities.invokeLater(() -> {calcularPosicionesNodos(); panelGrafo.repaint();});
    }

    // --- Panel de controles ---

    private JPanel crearPanelControles() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(30, 35, 45));
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 80, 120)),
                "Controles", 0, 0, null, Color.LIGHT_GRAY));

        // Simulación
        JButton btnTick   = boton("▶ Un tick",          new Color(40, 100, 160));
        JButton btnAuto   = boton("⏩ Auto",             new Color(40, 130, 80));
        JButton btnCortar = boton("✂ Optimizar cortes", new Color(160, 50, 50));

        // Grafo
        JButton btnAgregar = boton("➕ Nueva localidad",  new Color(50, 100, 55));
        JButton btnEditar  = boton("✏ Editar localidad", new Color(80, 55, 120));

        // Persistencia
        JButton btnImportar = boton("📂 Importar",  new Color(60, 90, 110));
        JButton btnExportar = boton("💾 Exportar",  new Color(60, 90, 110));

        // Reset
        JButton btnReset    = boton("↺ Reiniciar",       new Color(80, 80, 80));
        JButton btnEliminar = boton("🗑 Eliminar todo",   new Color(120, 40, 40));

        btnTick.addActionListener(e -> avanzarTick());
        btnAuto.addActionListener(e -> toggleAnimacion(btnAuto));
        btnCortar.addActionListener(e -> optimizarCortes());
        btnAgregar.addActionListener(e -> mostrarDialogoAgregarLocalidad());
        btnEditar.addActionListener(e -> mostrarDialogoEditarLocalidad());
        btnImportar.addActionListener(e -> importarGrafo());
        btnExportar.addActionListener(e -> exportarGrafo());
        btnReset.addActionListener(e -> reiniciar());
        btnEliminar.addActionListener(e -> eliminarTodo());

        sliderPresupuesto = crearSlider("Presupuesto $", 50, 500, 200);

        int gap = 4;
        p.add(Box.createVerticalStrut(gap));
        p.add(btnTick);    p.add(Box.createVerticalStrut(gap));
        p.add(btnAuto);    p.add(Box.createVerticalStrut(gap));
        p.add(btnCortar);  p.add(Box.createVerticalStrut(gap + 4));
        p.add(btnAgregar); p.add(Box.createVerticalStrut(gap));
        p.add(btnEditar);  p.add(Box.createVerticalStrut(gap + 4));
        p.add(btnImportar); p.add(Box.createVerticalStrut(gap));
        p.add(btnExportar); p.add(Box.createVerticalStrut(gap + 4));
        p.add(btnReset);    p.add(Box.createVerticalStrut(gap));
        p.add(btnEliminar); p.add(Box.createVerticalStrut(8));
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

    // =========================================================
    //  Acciones de simulación
    // =========================================================

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

    // Restaura el grafo al estado base (antes de ticks u optimizaciones)
    private void reiniciar() {
        if (timerSimulacion != null) timerSimulacion.stop();
        if (snapshotInicial != null) {
            grafo = snapshotInicial.clonarEstructura();
        } else {
            grafo = CargadorGrafo.cargarVacio();
        }
        motor          = new MotorEpidemia(grafo, modeloProp);
        nodoSeleccionado = -1;
        calcularPosicionesNodos();
        labelTick.setText("Tick: 0");
        actualizarInfo();
        panelGrafo.repaint();
    }

    // Borra todo y arranca con 1 localidad vacía
    private void eliminarTodo() {
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el grafo actual y empezar desde cero?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        if (timerSimulacion != null) timerSimulacion.stop();
        grafo            = CargadorGrafo.cargarVacio();
        snapshotInicial  = grafo.clonarEstructura();
        motor            = new MotorEpidemia(grafo, modeloProp);
        nodoSeleccionado = -1;
        calcularPosicionesNodos();
        labelTick.setText("Tick: 0");
        actualizarInfo();
        panelGrafo.repaint();
    }

    // =========================================================
    //  Importar / Exportar
    // =========================================================

    // Importa un grafo desde JSON o CSV; reemplaza el actual
    private void importarGrafo() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Importar grafo");
        fc.addChoosableFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("CSV (*.csv)",   "csv"));
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        try {
            GrafoEpidemia importado;
            if (f.getName().toLowerCase().endsWith(".csv"))
                importado = CargadorGrafo.importarCSV(f);
            else
                importado = CargadorGrafo.importarJSON(f);

            if (timerSimulacion != null) timerSimulacion.stop();
            grafo           = importado;
            snapshotInicial = grafo.clonarEstructura();
            motor           = new MotorEpidemia(grafo, modeloProp);
            nodoSeleccionado = -1;
            calcularPosicionesNodos();
            labelTick.setText("Tick: 0");
            actualizarInfo();
            panelGrafo.repaint();
            JOptionPane.showMessageDialog(this, "Grafo importado: " + f.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al importar:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Exporta el grafo actual a JSON o CSV
    private void exportarGrafo() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar grafo");
        fc.addChoosableFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("CSV (*.csv)",   "csv"));
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File   f      = fc.getSelectedFile();
        String nombre = f.getAbsolutePath();
        boolean esCSV = ((FileNameExtensionFilter) fc.getFileFilter())
                .getExtensions()[0].equals("csv");

        if (esCSV && !nombre.toLowerCase().endsWith(".csv"))  nombre += ".csv";
        if (!esCSV && !nombre.toLowerCase().endsWith(".json")) nombre += ".json";
        f = new File(nombre);

        try {
            if (esCSV) CargadorGrafo.exportarCSV(grafo, f);
            else       CargadorGrafo.exportarJSON(grafo, f);
            JOptionPane.showMessageDialog(this, "Grafo exportado: " + f.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al exportar:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    //  Snapshot
    // =========================================================

    // Guarda el estado actual como punto de retorno del reinicio
    private void guardarSnapshot() {
        snapshotInicial = grafo.clonarEstructura();
    }

    // =========================================================
    //  Diálogo: agregar localidad
    // =========================================================

    private void mostrarDialogoAgregarLocalidad() {
        JDialog dlg = new JDialog(this, "Nueva Localidad", true);
        dlg.setLayout(new BorderLayout(10, 10));
        Color bg = new Color(30, 35, 45);
        dlg.getContentPane().setBackground(bg);

        JPanel pProps = panelTitulado("Propiedades", bg);
        pProps.setLayout(new GridBagLayout());
        GridBagConstraints g = gbc();
        JTextField tfNombre     = campo("Nueva Ciudad");
        JSpinner   spPoblacion  = spinner(500, 1, 1_000_000, 100);
        JSpinner   spInfectados = spinner(0, 0, 1_000_000, 1);
        spPoblacion.addChangeListener(e -> {
            int max = (int) spPoblacion.getValue();
            ((SpinnerNumberModel) spInfectados.getModel()).setMaximum(max);
            if ((int) spInfectados.getValue() > max) spInfectados.setValue(max);
        });

        g.gridx=0; g.gridy=0; pProps.add(lbl("Nombre:"), g);
        g.gridx=1;             pProps.add(tfNombre, g);
        g.gridx=0; g.gridy=1; pProps.add(lbl("Población:"), g);
        g.gridx=1;             pProps.add(spPoblacion, g);
        g.gridx=0; g.gridy=2; pProps.add(lbl("Infectados:"), g);
        g.gridx=1;             pProps.add(spInfectados, g);

        int n = grafo.getNumLocalidades();
        JCheckBox[] checks = new JCheckBox[n];
        JSpinner[]  sPeso  = new JSpinner[n];
        JSpinner[]  sMov   = new JSpinner[n];
        JSpinner[]  sCosto = new JSpinner[n];
        JPanel pConRows = new JPanel();
        pConRows.setLayout(new BoxLayout(pConRows, BoxLayout.Y_AXIS));
        pConRows.setBackground(bg);

        for (int i = 0; i < n; i++) {
            checks[i] = new JCheckBox(grafo.getLocalidad(i).nombre);
            checks[i].setForeground(Color.LIGHT_GRAY);
            checks[i].setBackground(bg);
            sPeso[i]  = spinner(5, 1, 9999, 1);
            sMov[i]   = spinnerDouble(0.20, 0.01, 1.0, 0.05);
            sCosto[i] = spinner(100, 1, 9999, 10);
            sPeso[i].setEnabled(false); sMov[i].setEnabled(false); sCosto[i].setEnabled(false);
            final int fi = i;
            checks[fi].addActionListener(e -> {
                boolean sel = checks[fi].isSelected();
                sPeso[fi].setEnabled(sel); sMov[fi].setEnabled(sel); sCosto[fi].setEnabled(sel);
            });
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            row.setBackground(bg);
            row.add(checks[i]);
            row.add(lbl("P:")); row.add(sPeso[i]);
            row.add(lbl("Mov:")); row.add(sMov[i]);
            row.add(lbl("$:")); row.add(sCosto[i]);
            pConRows.add(row);
        }

        JScrollPane scroll = estilizarScroll(new JScrollPane(pConRows), bg);
        scroll.setPreferredSize(new Dimension(430, Math.min(n * 38 + 10, 200)));
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 80, 120)),
                "Conexiones", 0, 0, null, Color.LIGHT_GRAY));

        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pBtns.setBackground(bg);
        JButton btnOk  = boton("Aceptar",  new Color(40, 120, 80));
        JButton btnCan = boton("Cancelar", new Color(80, 80, 80));
        btnCan.addActionListener(e -> dlg.dispose());
        btnOk.addActionListener(e -> {
            String nombre = tfNombre.getText().trim();
            if (nombre.isEmpty()) { JOptionPane.showMessageDialog(dlg, "El nombre no puede estar vacío."); return; }
            int pob   = (int) spPoblacion.getValue();
            int inf   = Math.min((int) spInfectados.getValue(), pob);
            int newId = grafo.getNumLocalidades();
            Localidad nueva = new Localidad(newId, nombre, pob);
            nueva.semillaInicial(inf);
            grafo.agregarLocalidadNueva(nueva);
            for (int i = 0; i < n; i++) {
                if (checks[i].isSelected())
                    grafo.agregarConexion(new Conexion(newId, i,
                            (int) sPeso[i].getValue(), (double) sMov[i].getValue(), (int) sCosto[i].getValue()));
            }
            guardarSnapshot();
            calcularPosicionesNodos();
            actualizarInfo();
            panelGrafo.repaint();
            dlg.dispose();
        });
        pBtns.add(btnCan); pBtns.add(btnOk);

        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.setBackground(bg);
        centro.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));
        centro.add(pProps, BorderLayout.NORTH);
        centro.add(scroll, BorderLayout.CENTER);
        dlg.add(centro, BorderLayout.CENTER);
        dlg.add(pBtns,  BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // =========================================================
    //  Diálogo: editar localidad seleccionada
    // =========================================================

    private void mostrarDialogoEditarLocalidad() {
        if (nodoSeleccionado < 0) {
            JOptionPane.showMessageDialog(this,
                    "Haz clic sobre un nodo del grafo para seleccionarlo primero.",
                    "Sin selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = nodoSeleccionado;
        Localidad loc = grafo.getLocalidad(id);

        JDialog dlg = new JDialog(this, "Editar: " + loc.nombre, true);
        dlg.setLayout(new BorderLayout(10, 10));
        Color bg = new Color(30, 35, 45);
        dlg.getContentPane().setBackground(bg);

        JPanel pProps = panelTitulado("Propiedades", bg);
        pProps.setLayout(new GridBagLayout());
        GridBagConstraints g = gbc();
        JTextField tfNombre     = campo(loc.nombre);
        JSpinner   spPoblacion  = spinner(loc.poblacion, 1, 1_000_000, 100);
        JSpinner   spInfectados = spinner(loc.infectados, 0, loc.poblacion, 1);
        spPoblacion.addChangeListener(e -> {
            int max = (int) spPoblacion.getValue();
            ((SpinnerNumberModel) spInfectados.getModel()).setMaximum(max);
            if ((int) spInfectados.getValue() > max) spInfectados.setValue(max);
        });

        g.gridx=0; g.gridy=0; pProps.add(lbl("Nombre:"), g);
        g.gridx=1;             pProps.add(tfNombre, g);
        g.gridx=0; g.gridy=1; pProps.add(lbl("Población:"), g);
        g.gridx=1;             pProps.add(spPoblacion, g);
        g.gridx=0; g.gridy=2; pProps.add(lbl("Infectados:"), g);
        g.gridx=1;             pProps.add(spInfectados, g);

        // Filas de conexiones editables
        class FilaConexion {
            int destino; JSpinner spPeso, spMov, spCosto; JPanel panel;
        }
        List<FilaConexion> filas = new ArrayList<>();
        JPanel pConRows = new JPanel();
        pConRows.setLayout(new BoxLayout(pConRows, BoxLayout.Y_AXIS));
        pConRows.setBackground(bg);

        Runnable rebuild = () -> {
            pConRows.removeAll();
            for (FilaConexion f : filas) pConRows.add(f.panel);
            pConRows.revalidate(); pConRows.repaint(); dlg.pack();
        };

        for (Conexion c : grafo.getAdj(id)) {
            FilaConexion f  = new FilaConexion();
            f.destino = c.destino;
            f.spPeso  = spinner((int) c.peso, 1, 9999, 1);
            f.spMov   = spinnerDouble(c.tasaMovilidad, 0.01, 1.0, 0.05);
            f.spCosto = spinner(c.costoCorte, 1, 9999, 10);
            f.panel   = filaConexionPanel(bg, grafo.getLocalidad(c.destino).nombre,
                    f.spPeso, f.spMov, f.spCosto, () -> { filas.remove(f); rebuild.run(); });
            filas.add(f);
        }

        JScrollPane scrollExist = estilizarScroll(new JScrollPane(pConRows), bg);
        scrollExist.setPreferredSize(new Dimension(460, Math.min(filas.size() * 38 + 10, 180)));
        scrollExist.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 80, 120)),
                "Conexiones actuales", 0, 0, null, Color.LIGHT_GRAY));
        rebuild.run();

        // Panel para agregar nueva conexión
        JPanel pAdd = panelTitulado("Agregar conexión", bg);
        pAdd.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 4));

        String[] opciones = new String[grafo.getNumLocalidades()];
        int[]    opIdx    = new int[grafo.getNumLocalidades()];
        int cnt = 0;
        for (int i = 0; i < grafo.getNumLocalidades(); i++)
            if (i != id) { opciones[cnt] = grafo.getLocalidad(i).nombre; opIdx[cnt++] = i; }
        opciones = Arrays.copyOf(opciones, cnt);
        final int[] opIdxF = Arrays.copyOf(opIdx, cnt);

        JComboBox<String> cbDest  = new JComboBox<>(opciones);
        cbDest.setBackground(new Color(45, 50, 65)); cbDest.setForeground(Color.WHITE);
        JSpinner spNPeso  = spinner(3, 1, 9999, 1);
        JSpinner spNMov   = spinnerDouble(0.20, 0.01, 1.0, 0.05);
        JSpinner spNCosto = spinner(100, 1, 9999, 10);
        JButton  btnAdd   = boton("➕", new Color(40, 120, 80));
        btnAdd.setMaximumSize(new Dimension(50, 26));
        btnAdd.addActionListener(e -> {
            if (cbDest.getItemCount() == 0) return;
            int dst = opIdxF[cbDest.getSelectedIndex()];
            if (filas.stream().anyMatch(f -> f.destino == dst)) {
                JOptionPane.showMessageDialog(dlg, "Ya existe esa conexión."); return;
            }
            FilaConexion f  = new FilaConexion();
            f.destino = dst;
            f.spPeso  = spinner((int) spNPeso.getValue(), 1, 9999, 1);
            f.spMov   = spinnerDouble((double) spNMov.getValue(), 0.01, 1.0, 0.05);
            f.spCosto = spinner((int) spNCosto.getValue(), 1, 9999, 10);
            f.panel   = filaConexionPanel(bg, grafo.getLocalidad(dst).nombre,
                    f.spPeso, f.spMov, f.spCosto, () -> { filas.remove(f); rebuild.run(); });
            filas.add(f); rebuild.run();
        });

        pAdd.add(lbl("Destino:")); pAdd.add(cbDest);
        pAdd.add(lbl("P:")); pAdd.add(spNPeso);
        pAdd.add(lbl("Mov:")); pAdd.add(spNMov);
        pAdd.add(lbl("$:")); pAdd.add(spNCosto);
        pAdd.add(btnAdd);

        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pBtns.setBackground(bg);
        JButton btnOk  = boton("Aceptar",  new Color(40, 120, 80));
        JButton btnCan = boton("Cancelar", new Color(80, 80, 80));
        btnCan.addActionListener(e -> dlg.dispose());
        btnOk.addActionListener(e -> {
            String nombre = tfNombre.getText().trim();
            if (nombre.isEmpty()) { JOptionPane.showMessageDialog(dlg, "Nombre vacío."); return; }
            int pob = (int) spPoblacion.getValue();
            int inf = Math.min((int) spInfectados.getValue(), pob);
            Localidad nueva = new Localidad(id, nombre, pob);
            nueva.semillaInicial(inf);
            grafo.reemplazarLocalidad(id, nueva);
            grafo.eliminarTodasConexiones(id);
            for (FilaConexion f : filas)
                grafo.agregarConexion(new Conexion(id, f.destino,
                        (int) f.spPeso.getValue(), (double) f.spMov.getValue(), (int) f.spCosto.getValue()));
            guardarSnapshot();
            actualizarInfo();
            panelGrafo.repaint();
            dlg.dispose();
        });
        pBtns.add(btnCan); pBtns.add(btnOk);

        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.setBackground(bg);
        centro.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));
        centro.add(pProps,      BorderLayout.NORTH);
        centro.add(scrollExist, BorderLayout.CENTER);
        centro.add(pAdd,        BorderLayout.SOUTH);
        dlg.add(centro, BorderLayout.CENTER);
        dlg.add(pBtns,  BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // =========================================================
    //  Detección de clic en nodo
    // =========================================================

    private void detectarClicNodo(int x, int y) {
        final int R = 36;
        for (int i = 0; i < grafo.getNumLocalidades(); i++) {
            Point p = posiciones.get(i);
            if (p != null) {
                int dx = x - p.x, dy = y - p.y;
                if (dx * dx + dy * dy <= R * R) {
                    nodoSeleccionado = (nodoSeleccionado == i) ? -1 : i;
                    panelGrafo.repaint();
                    return;
                }
            }
        }
        nodoSeleccionado = -1;
        panelGrafo.repaint();
    }

    // =========================================================
    //  Dibujo
    // =========================================================

    private void calcularPosicionesNodos() {
        posiciones.clear();

        int n = grafo.getNumLocalidades();
        if (n == 0) return;

        int w = (panelGrafo != null) ? panelGrafo.getWidth() : 0;
        int h = (panelGrafo != null) ? panelGrafo.getHeight() : 0;

        if (w <= 0 || h <= 0) {
            w = getWidth();
            h = getHeight();
        }

        int cx = w / 2;
        int cy = h / 2;

        int margen = 80;
        int radioMax = Math.min(w, h) / 2 - margen;
        int radio = Math.min(200, Math.max(60, Math.min(radioMax, 80 + n * 12)));

        for (int i = 0; i < n; i++) {
            double ang = 2 * Math.PI * i / n - Math.PI / 2;
            posiciones.put(i, new Point(
                    (int) (cx + radio * Math.cos(ang)),
                    (int) (cy + radio * Math.sin(ang))
            ));
        }
    }

    // Panel de dibujo del grafo
    class PanelGrafo extends JPanel {

        PanelGrafo() {
            setBackground(new Color(15, 20, 30));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { detectarClicNodo(e.getX(), e.getY()); }
            });
            
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    calcularPosicionesNodos();
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
                    if (p1 == null || p2 == null) continue;
                    if (c.cortada) {
                        g2.setColor(new Color(180, 50, 50, 160));
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER, 10, new float[]{7, 7}, 0));
                    } else {
                        g2.setColor(new Color(90, 130, 200, 180));
                        g2.setStroke(new BasicStroke((float)(c.tasaMovilidad * 5 + 1)));
                    }
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                    g2.setColor(new Color(160, 160, 160));
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
                    g2.drawString(String.valueOf(c.peso), (p1.x + p2.x) / 2 + 4, (p1.y + p2.y) / 2 - 4);
                }
            }
        }

        private void dibujarNodos(Graphics2D g2) {
        final int R = 48;

        for (int i = 0; i < grafo.getNumLocalidades(); i++) {
            Localidad l = grafo.getLocalidad(i);
            Point p = posiciones.get(i);

            if (p == null || l == null) continue;
            Color fill = colorPorFraccion(l.fraccionInfectada());
            // sombra
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillOval(p.x - R + 3, p.y - R + 3, R * 2, R * 2);

            // =========================
            // IMAGEN DEL MAPA
            // =========================
            BufferedImage img = cargarImagenCiudad(l.nombre);

            if (img != null) {
                Shape clipOriginal = g2.getClip();
                g2.setClip(new java.awt.geom.Ellipse2D.Double(
                    p.x - R,
                    p.y - R,
                    R * 2,
                    R * 2
                ));

                g2.drawImage(
                    img,
                    p.x - R,
                    p.y - R,
                    R * 2,
                    R * 2,
                    null
                );
                g2.setClip(clipOriginal);

            } else {
                // fallback si no hay imagen
                g2.setColor(fill);
                g2.fillOval(p.x - R, p.y - R, R * 2, R * 2);
            }
            
            Color c = colorPorFraccion(l.fraccionInfectada());
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));
            g2.setStroke(new BasicStroke(6f));
            g2.drawOval(p.x - R, p.y - R, R * 2, R * 2);

            // borde del nodo
            if (i == nodoSeleccionado) {
                g2.setColor(new Color(0, 230, 200));
                g2.setStroke(new BasicStroke(3.5f));

            } else {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
            }

            g2.drawOval(p.x - R, p.y - R, R * 2, R * 2);

            // nombre ciudad
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));

            FontMetrics fm = g2.getFontMetrics();

            String nombre = l.nombre.split(" ")[0];

            g2.drawString(
                    nombre,
                    p.x - fm.stringWidth(nombre) / 2,
                    p.y
            );

            // porcentaje infectado
            String pct = String.format("%.0f%%", l.fraccionInfectada() * 100);

            g2.setFont(new Font("Monospaced", Font.BOLD, 11));
            fm = g2.getFontMetrics();

            g2.setColor(new Color(255, 230, 150));

            g2.drawString(
                    pct,
                    p.x - fm.stringWidth(pct) / 2,
                    p.y + 15
            );

            // infectados / población
            String sub = l.infectados + "/" + l.poblacion;

            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            fm = g2.getFontMetrics();

            g2.setColor(new Color(190, 190, 190));

            g2.drawString(
                    sub,
                    p.x - fm.stringWidth(sub) / 2,
                    p.y + R + 15
            );
        }
        }

            // Interpolación azul → amarillo → rojo según fracción infectada
            private Color colorPorFraccion(double f) {
                f = Math.max(0, Math.min(1, f));
                if (f < 0.5) {
                    return new Color((int)(f*2*200), (int)(f*2*160+60), (int)((1-f*2)*160+20));
                } else {
                    double t = (f - 0.5) * 2;
                    return new Color(200, (int)((1-t)*160), 20);
                }
            }

            private void dibujarLeyenda(Graphics2D g2) {
                int x = 12, y = 20;
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                for (int i = 0; i <= 4; i++) {
                    g2.setColor(colorPorFraccion(i / 4.0));
                    g2.fillRect(x + i * 28, y, 28, 12);
                }
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(x, y, 140, 12);
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString("0%",   x,       y + 26);
                g2.drawString("50%",  x + 57,  y + 26);
                g2.drawString("100%", x + 108, y + 26);
                g2.drawString("infectados", x + 20, y + 40);
            }
        }

    // =========================================================
    //  Info panel
    // =========================================================

    private void actualizarInfo() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < grafo.getNumLocalidades(); i++) {
            Localidad l   = grafo.getLocalidad(i);
            if (l == null) continue;   // guardar contra IDs no secuenciales
            int       pct = (int)(l.fraccionInfectada() * 100);
            int       lls = pct / 10;
            String    bar = "[" + "█".repeat(lls) + "░".repeat(10 - lls) + "]";
            sb.append(String.format("%-13s%n%s %d%%%n%d / %d%n%n",
                    l.nombre, bar, pct, l.infectados, l.poblacion));
        }
        areaInfo.setText(sb.toString());
    }

    // =========================================================
    //  Helpers de UI
    // =========================================================

    // Panel de una fila de conexión con campos y botón eliminar
    private JPanel filaConexionPanel(Color bg, String nombreDest,
                                     JSpinner spPeso, JSpinner spMov, JSpinner spCosto,
                                     Runnable onDelete) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBackground(bg);
        row.add(lbl(String.format("%-14s", nombreDest)));
        row.add(lbl("P:")); row.add(spPeso);
        row.add(lbl("Mov:")); row.add(spMov);
        row.add(lbl("$:")); row.add(spCosto);
        JButton del = boton("🗑", new Color(140, 40, 40));
        del.setMaximumSize(new Dimension(40, 26));
        del.addActionListener(e -> onDelete.run());
        row.add(del);
        return row;
    }

    private JLabel lbl(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Color.LIGHT_GRAY);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return l;
    }

    private JTextField campo(String valor) {
        JTextField tf = new JTextField(valor, 14);
        tf.setBackground(new Color(45, 50, 65));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createLineBorder(new Color(70, 90, 130)));
        return tf;
    }

    private JSpinner spinner(int val, int min, int max, int step) {
        int safeMin = Math.min(min, max);
        int safeMax = Math.max(min, max);
        int safeVal = Math.max(safeMin, Math.min(safeMax, val));
        JSpinner sp = new JSpinner(new SpinnerNumberModel(safeVal, safeMin, safeMax, Math.max(1, step)));
        sp.setPreferredSize(new Dimension(75, 24));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setBackground(new Color(45, 50, 65));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setForeground(Color.WHITE);
        return sp;
    }

    private JSpinner spinnerDouble(double val, double min, double max, double step) {
        double safeMin = Math.min(min, max);
        double safeMax = Math.max(min, max);
        double safeVal = Double.isFinite(val) ? Math.max(safeMin, Math.min(safeMax, val)) : safeMin;
        JSpinner sp = new JSpinner(new SpinnerNumberModel(safeVal, safeMin, safeMax, step <= 0 ? 0.01 : step));
        sp.setPreferredSize(new Dimension(70, 24));
        sp.setEditor(new JSpinner.NumberEditor(sp, "0.00"));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setBackground(new Color(45, 50, 65));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setForeground(Color.WHITE);
        return sp;
    }

    private JPanel panelTitulado(String titulo, Color bg) {
        JPanel p = new JPanel();
        p.setBackground(bg);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 80, 120)),
                titulo, 0, 0, null, Color.LIGHT_GRAY));
        return p;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;
        return g;
    }

    // Aplica tema oscuro al JScrollPane y su barra
    private JScrollPane estilizarScroll(JScrollPane sp, Color bg) {
        sp.setBackground(bg);
        sp.setOpaque(true);
        sp.getViewport().setBackground(bg);
        sp.getViewport().setOpaque(true);
        for (JScrollBar bar : new JScrollBar[]{
                sp.getVerticalScrollBar(), sp.getHorizontalScrollBar()}) {
            bar.setBackground(bg);
            bar.setOpaque(true);
            bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                @Override protected void configureScrollBarColors() {
                    thumbColor = new Color(80, 100, 145);
                    trackColor = new Color(35, 40, 55);
                }
                @Override protected JButton createDecreaseButton(int o) { return botonScrollVacio(); }
                @Override protected JButton createIncreaseButton(int o) { return botonScrollVacio(); }
                private JButton botonScrollVacio() {
                    JButton b = new JButton();
                    b.setPreferredSize(new Dimension(0, 0));
                    b.setMinimumSize(new Dimension(0, 0));
                    b.setMaximumSize(new Dimension(0, 0));
                    return b;
                }
            });
        }
        return sp;
    }

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
