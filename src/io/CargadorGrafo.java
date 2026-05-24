package io;

import modelo.Conexion;
import modelo.GrafoEpidemia;
import modelo.Localidad;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Carga, importa y exporta grafos en formato JSON y CSV
public class CargadorGrafo {

    // --- Grafo vacío con una sola localidad inicial ---
    public static GrafoEpidemia cargarVacio() {
        GrafoEpidemia g = new GrafoEpidemia(1);
        g.agregarLocalidad(new Localidad(0, "Ciudad 1", 1000));
        return g;
    }

    // --- Grafo de ejemplo (5 ciudades colombianas) ---
    public static GrafoEpidemia cargarEjemplo() {
        GrafoEpidemia g = new GrafoEpidemia(5);
        g.agregarLocalidad(new Localidad(0, "Bogotá", 1000));
        g.agregarLocalidad(new Localidad(1, "Medellín", 600));
        g.agregarLocalidad(new Localidad(2, "Cali", 500));
        g.agregarLocalidad(new Localidad(3, "Barranquilla", 400));
        g.agregarLocalidad(new Localidad(4, "Bucaramanga", 300));
        g.agregarConexion(new Conexion(0, 1, 4, 0.30, 200));
        g.agregarConexion(new Conexion(0, 2, 3, 0.25, 150));
        g.agregarConexion(new Conexion(1, 2, 1, 0.40, 80));
        g.agregarConexion(new Conexion(1, 3, 2, 0.20, 100));
        g.agregarConexion(new Conexion(2, 3, 4, 0.15, 90));
        g.agregarConexion(new Conexion(3, 4, 2, 0.10, 60));
        g.getLocalidad(0).semillaInicial(10);
        return g;
    }

    // =========================================================
    //  EXPORTAR
    // =========================================================

    // Exporta el grafo a JSON
    public static void exportarJSON(GrafoEpidemia g, File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\"localidades\": [\n");
        int n = g.getNumLocalidades();
        for (int i = 0; i < n; i++) {
            Localidad l = g.getLocalidad(i);
            if (l == null) continue;
            sb.append(String.format(Locale.US,
                    "{\"id\":%d,\"nombre\":\"%s\",\"poblacion\":%d,\"infectados\":%d}",
                    l.id, escaparJSON(l.nombre), l.poblacion, l.infectados));
            if (i < n - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("],\n");
        sb.append("\"conexiones\": [\n");

        List<Conexion> aristas = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (Conexion c : g.getAdj(i)) {
                if (c.origen < c.destino) aristas.add(c);
            }
        }

        for (int i = 0; i < aristas.size(); i++) {
            Conexion c = aristas.get(i);
            sb.append(String.format(Locale.US,
                    "{\"origen\":%d,\"destino\":%d,\"peso\":%d,\"movilidad\":%.4f,\"costo\":%d,\"cortada\":%b}",
                    c.origen, c.destino, c.peso, c.tasaMovilidad, c.costoCorte, c.cortada));
            if (i < aristas.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n}");

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.print(sb.toString());
        }
    }

    // Exporta el grafo a CSV
    public static void exportarCSV(GrafoEpidemia g, File f) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("id,nombre,poblacion,infectados");
            int n = g.getNumLocalidades();
            for (int i = 0; i < n; i++) {
                Localidad l = g.getLocalidad(i);
                if (l == null) continue;
                String nombreEscapado = l.nombre.replace("\"", "\"\"");
                pw.printf(Locale.US, "%d,\"%s\",%d,%d%n", l.id, nombreEscapado, l.poblacion, l.infectados);
            }
            pw.println("#conexiones");
            pw.println("origen,destino,peso,movilidad,costo,cortada");
            for (int i = 0; i < n; i++) {
                for (Conexion c : g.getAdj(i)) {
                    if (c.origen < c.destino) {
                        pw.printf(Locale.US, "%d,%d,%d,%.4f,%d,%b%n",
                                c.origen, c.destino, c.peso, c.tasaMovilidad, c.costoCorte, c.cortada);
                    }
                }
            }
        }
    }

    // =========================================================
    //  IMPORTAR
    // =========================================================

    // Importa un grafo desde JSON
    public static GrafoEpidemia importarJSON(File f) throws IOException {
        String contenido = leerArchivo(f);

        List<Map<String, String>> locs = extraerObjetosJSON(contenido, "localidades");
        List<Map<String, String>> cons = extraerObjetosJSON(contenido, "conexiones");

        if (locs.isEmpty()) throw new IOException("El archivo no contiene localidades.");

        List<Localidad> localidades = new ArrayList<>();
        for (Map<String, String> m : locs) {
            int id = parseEnteroSeguro(m.get("id"), 0);
            String nom = m.getOrDefault("nombre", "Localidad " + id);
            int pob = parseEnteroSeguro(m.get("poblacion"), 1);
            int inf = parseEnteroSeguro(m.getOrDefault("infectados", "0"), 0);
            Localidad l = new Localidad(id, nom, pob);
            l.semillaInicial(inf);
            localidades.add(l);
        }

        List<Conexion> conexiones = new ArrayList<>();
        for (Map<String, String> m : cons) {
            int or = parseEnteroSeguro(m.get("origen"), -1);
            int dst = parseEnteroSeguro(m.get("destino"), -1);
            int p = parseEnteroSeguro(m.get("peso"), 1);
            double mov = parseDoubleSeguro(m.get("movilidad"), 0.20);
            int co = parseEnteroSeguro(m.get("costo"), 1);
            boolean cor = Boolean.parseBoolean(m.getOrDefault("cortada", "false"));
            Conexion c = new Conexion(or, dst, p, mov, co);
            c.cortada = cor;
            conexiones.add(c);
        }

        return construirGrafoNormalizado(localidades, conexiones);
    }

    // Importa un grafo desde CSV
    public static GrafoEpidemia importarCSV(File f) throws IOException {
        List<String> lineas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String l;
            while ((l = br.readLine()) != null) {
                lineas.add(l.trim());
            }
        }

        List<Localidad> localidades = new ArrayList<>();
        List<Conexion> conexiones = new ArrayList<>();
        boolean modoConexiones = false;

        for (String linea : lineas) {
            if (linea.isEmpty()) continue;
            if (linea.equalsIgnoreCase("#conexiones")) {
                modoConexiones = true;
                continue;
            }
            if (linea.startsWith("id,") || linea.startsWith("origen,")) continue;

            String[] p = parsearLineaCSV(linea);
            if (!modoConexiones) {
                if (p.length < 4) continue;
                int id = parseEnteroSeguro(p[0], 0);
                String nom = desescaparCSV(p[1]);
                int pob = parseEnteroSeguro(p[2], 1);
                int inf = parseEnteroSeguro(p[3], 0);
                Localidad loc = new Localidad(id, nom, pob);
                loc.semillaInicial(inf);
                localidades.add(loc);
            } else {
                if (p.length < 5) continue;
                int or = parseEnteroSeguro(p[0], -1);
                int dst = parseEnteroSeguro(p[1], -1);
                int pe = parseEnteroSeguro(p[2], 1);
                double mov = parseDoubleSeguro(p[3], 0.20);
                int co = parseEnteroSeguro(p[4], 1);
                boolean cor = p.length > 5 && Boolean.parseBoolean(p[5].trim());
                Conexion c = new Conexion(or, dst, pe, mov, co);
                c.cortada = cor;
                conexiones.add(c);
            }
        }

        if (localidades.isEmpty()) throw new IOException("El archivo no contiene localidades.");
        return construirGrafoNormalizado(localidades, conexiones);
    }

    // =========================================================
    //  Auxiliares
    // =========================================================

    // Reordena y normaliza ids para que el grafo siempre quede 0..n-1
    private static GrafoEpidemia construirGrafoNormalizado(List<Localidad> localidades,
                                                           List<Conexion> conexiones) throws IOException {
        Map<Integer, Localidad> porId = new TreeMap<>();
        for (Localidad l : localidades) {
            if (l != null) porId.put(l.id, l);
        }
        if (porId.isEmpty()) throw new IOException("El archivo no contiene localidades válidas.");

        GrafoEpidemia g = new GrafoEpidemia(porId.size());
        Map<Integer, Integer> remapeo = new LinkedHashMap<>();
        int idx = 0;
        for (Localidad original : porId.values()) {
            remapeo.put(original.id, idx);
            Localidad copia = new Localidad(idx, original.nombre, original.poblacion);
            copia.semillaInicial(original.infectados);
            g.agregarLocalidad(copia);
            idx++;
        }

        Map<Long, Conexion> unicas = new LinkedHashMap<>();
        for (Conexion c : conexiones) {
            Integer or = remapeo.get(c.origen);
            Integer dst = remapeo.get(c.destino);
            if (or == null || dst == null || or.equals(dst)) continue;

            int a = Math.min(or, dst);
            int b = Math.max(or, dst);
            long key = (((long) a) << 32) | (b & 0xffffffffL);

            Conexion existente = unicas.get(key);
            if (existente == null) {
                Conexion copia = new Conexion(a, b,
                        clampEntero(c.peso, 1, 9999),
                        clampDouble(c.tasaMovilidad, 0.01, 1.0),
                        clampEntero(c.costoCorte, 1, 999999));
                copia.cortada = c.cortada;
                unicas.put(key, copia);
            } else if (c.cortada) {
                existente.cortada = true;
            }
        }

        for (Conexion c : unicas.values()) {
            g.agregarConexion(c);
        }
        return g;
    }

    private static String escaparJSON(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String desescaparCSV(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }
        return t.replace("\"\"", "\"");
    }

    private static String leerArchivo(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String l;
            while ((l = br.readLine()) != null) sb.append(l).append('\n');
        }
        return sb.toString();
    }

    // Extrae objetos JSON de un array nombrado usando escaneo por llaves
    private static List<Map<String, String>> extraerObjetosJSON(String json, String clave) {
        List<Map<String, String>> lista = new ArrayList<>();
        String buscada = "\"" + clave + "\"";
        int idx = json.indexOf(buscada);
        if (idx < 0) return lista;

        int ini = json.indexOf('[', idx);
        if (ini < 0) return lista;

        int fin = encontrarCierre(json, ini, '[', ']');
        if (fin < 0) return lista;

        String bloque = json.substring(ini + 1, fin);
        int i = 0;
        while (i < bloque.length()) {
            while (i < bloque.length() && Character.isWhitespace(bloque.charAt(i))) i++;
            if (i >= bloque.length()) break;
            if (bloque.charAt(i) != '{') {
                i++;
                continue;
            }
            int j = encontrarCierre(bloque, i, '{', '}');
            if (j < 0) break;
            lista.add(parseJsonCampos(bloque.substring(i + 1, j)));
            i = j + 1;
        }
        return lista;
    }

    private static int encontrarCierre(String texto, int inicio, char abre, char cierra) {
        int nivel = 0;
        boolean enCadena = false;
        boolean escape = false;
        for (int i = inicio; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            if (enCadena) {
                if (escape) {
                    escape = false;
                } else if (ch == '\\') {
                    escape = true;
                } else if (ch == '\"') {
                    enCadena = false;
                }
                continue;
            }
            if (ch == '\"') {
                enCadena = true;
            } else if (ch == abre) {
                nivel++;
            } else if (ch == cierra) {
                nivel--;
                if (nivel == 0) return i;
            }
        }
        return -1;
    }

    // Parsea los campos clave:valor de un objeto JSON simple (sin anidamiento)
    private static Map<String, String> parseJsonCampos(String obj) {
        Map<String, String> m = new LinkedHashMap<>();
        Pattern p = Pattern.compile("\"(\\w+)\"\\s*:\\s*(\"((?:\\\\.|[^\"])*)\"|([^,}\\s]+))");
        Matcher mt = p.matcher(obj);
        while (mt.find()) {
            String k = mt.group(1);
            String v = mt.group(3) != null ? desescaparJSON(mt.group(3)) : mt.group(4);
            m.put(k, v == null ? "" : v.trim());
        }
        return m;
    }

    private static String desescaparJSON(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    // Parsea una línea CSV respetando campos entre comillas
    private static String[] parsearLineaCSV(String linea) {
        List<String> campos = new ArrayList<>();
        boolean enComillas = false;
        StringBuilder actual = new StringBuilder();
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (enComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    enComillas = !enComillas;
                }
            } else if (c == ',' && !enComillas) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos.toArray(new String[0]);
    }

    private static int parseEnteroSeguro(String valor, int defecto) {
        if (valor == null) return defecto;
        try {
            return Integer.parseInt(valor.trim());
        } catch (Exception e) {
            try {
                return (int) Math.round(Double.parseDouble(valor.trim().replace(',', '.')));
            } catch (Exception ex) {
                return defecto;
            }
        }
    }

    private static double parseDoubleSeguro(String valor, double defecto) {
        if (valor == null) return defecto;
        try {
            return Double.parseDouble(valor.trim().replace(',', '.'));
        } catch (Exception e) {
            return defecto;
        }
    }

    private static int clampEntero(int valor, int min, int max) {
        return Math.max(min, Math.min(max, valor));
    }

    private static double clampDouble(double valor, double min, double max) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) return min;
        return Math.max(min, Math.min(max, valor));
    }
}
