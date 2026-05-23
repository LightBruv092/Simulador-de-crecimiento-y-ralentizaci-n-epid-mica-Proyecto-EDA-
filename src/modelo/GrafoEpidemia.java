package modelo;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

/**
 * RED GEOGRÁFICA COMPLETA.
 *
 * Extiende la lógica de tu Grafo original (listas de adyacencia, BFS, DFS,
 * Kruskal, Prim, Dijkstra, Floyd-Warshall) añadiendo la capa epidémica:
 * cada nodo es una Localidad con estado SEIR, y cada arista es una Conexion
 * con tasa de movilidad y costo de corte.
 *
 * REUTILIZACIÓN DE TUS ALGORITMOS:
 *   BFS/DFS       → detectar focos de infección y zonas alcanzables
 *   Kruskal/Prim  → árbol mínimo = red mínima para mantener conectividad
 *   Dijkstra      → camino crítico de propagación (el más rápido/peligroso)
 *   Floyd-Warshall → matriz completa de distancias entre todas las localidades
 */
public class GrafoEpidemia {

    private final int numLocalidades;
    private final LinkedList<Conexion>[] adj;  // lista de adyacencia
    private final Localidad[] localidades;

    @SuppressWarnings("unchecked")
    public GrafoEpidemia(int n) {
        this.numLocalidades = n;
        this.adj        = new LinkedList[n];
        this.localidades = new Localidad[n];
        for (int i = 0; i < n; i++) {
            adj[i] = new LinkedList<>();
        }
    }

    // =========================================================
    //  CONSTRUCCIÓN DEL GRAFO
    // =========================================================

    public void agregarLocalidad(Localidad l) {
        localidades[l.id] = l;
    }

    /**
     * Agrega una conexión bidireccional entre dos localidades.
     * Reutiliza la misma idea de aggAristasMejorado() de tu código.
     */
    public void agregarConexion(Conexion c) {
        // Arista en ambas direcciones (grafo no dirigido)
        adj[c.origen].add(c);
        adj[c.destino].add(new Conexion(c.destino, c.origen,
                c.peso, c.tasaMovilidad, c.costoCorte));
    }

    public Localidad getLocalidad(int i) { return localidades[i]; }
    public int getNumLocalidades()        { return numLocalidades; }
    public LinkedList<Conexion> getAdj(int v) { return adj[v]; }

    // =========================================================
    //  BFS — detectar todas las localidades alcanzadas desde un foco
    //  Uso epidémico: saber hasta dónde puede llegar el virus
    // =========================================================
    public List<Integer> bfsDesde(int origen) {
        boolean[] visitado = new boolean[numLocalidades];
        Queue<Integer> cola = new LinkedList<>();
        List<Integer> orden = new ArrayList<>();

        visitado[origen] = true;
        cola.offer(origen);

        while (!cola.isEmpty()) {
            int u = cola.poll();
            orden.add(u);

            for (Conexion c : adj[u]) {
                // Solo atraviesa conexiones activas (no cortadas)
                if (!visitado[c.destino] && c.estaActiva()) {
                    visitado[c.destino] = true;
                    cola.offer(c.destino);
                }
            }
        }
        return orden;
    }

    // =========================================================
    //  DFS — exploración profunda desde un foco
    //  Uso epidémico: identificar cadenas de transmisión
    // =========================================================
    public List<Integer> dfsDesde(int origen) {
        boolean[] visitado = new boolean[numLocalidades];
        Stack<Integer> pila = new Stack<>();
        List<Integer> orden = new ArrayList<>();

        pila.push(origen);

        while (!pila.isEmpty()) {
            int u = pila.pop();
            if (!visitado[u]) {
                visitado[u] = true;
                orden.add(u);
                for (Conexion c : adj[u]) {
                    if (!visitado[c.destino] && c.estaActiva()) {
                        pila.push(c.destino);
                    }
                }
            }
        }
        return orden;
    }

    // =========================================================
    //  KRUSKAL — árbol de expansión mínima por costos de corte
    //  Uso epidémico: encontrar qué conexiones son más baratas de eliminar
    //  El optimizador invierte la lógica: corta las aristas FUERA del MST
    // =========================================================
    public List<Conexion> kruskal() {
        // Recopilar aristas sin duplicados (solo u < v)
        List<Conexion> aristas = new ArrayList<>();
        for (int u = 0; u < numLocalidades; u++) {
            for (Conexion c : adj[u]) {
                if (u < c.destino && c.estaActiva()) {
                    aristas.add(c);
                }
            }
        }

        // Ordenar por peso (costo de la conexión)
        aristas.sort((a, b) -> a.peso - b.peso);

        int[] padre = new int[numLocalidades];
        int[] rank  = new int[numLocalidades];
        for (int i = 0; i < numLocalidades; i++) { padre[i] = i; }

        List<Conexion> mst = new ArrayList<>();

        for (Conexion c : aristas) {
            int x = find(padre, c.origen);
            int y = find(padre, c.destino);

            if (x != y) {
                mst.add(c);
                union(padre, rank, x, y);
                if (mst.size() == numLocalidades - 1) break;
            }
        }
        return mst;
    }

    // =========================================================
    //  PRIM — árbol de expansión mínima (alternativa a Kruskal)
    //  Uso epidémico: misma lógica, útil para grafos densos
    // =========================================================
    public int[] prim() {
        int[] peso  = new int[numLocalidades];
        int[] padre = new int[numLocalidades];
        boolean[] enMST = new boolean[numLocalidades];

        final int INF = Integer.MAX_VALUE;
        for (int i = 0; i < numLocalidades; i++) {
            peso[i]  = INF;
            padre[i] = -1;
        }
        peso[0] = 0;

        for (int i = 0; i < numLocalidades - 1; i++) {
            int u = minDistancia(peso, enMST);
            if (u == -1) break;
            enMST[u] = true;

            for (Conexion c : adj[u]) {
                if (!enMST[c.destino] && c.estaActiva() && c.peso < peso[c.destino]) {
                    padre[c.destino] = u;
                    peso[c.destino]  = c.peso;
                }
            }
        }
        return padre; // padre[i] = nodo anterior en el MST
    }

    // =========================================================
    //  DIJKSTRA — camino más corto desde un foco
    //  Uso epidémico: encontrar el camino de propagación más RÁPIDO
    //  (menor peso = mayor riesgo de llegada temprana)
    // =========================================================
    public int[] dijkstra(int origen) {
        final int INF = 999999;
        int[] dist    = new int[numLocalidades];
        int[] prev    = new int[numLocalidades];
        boolean[] vis = new boolean[numLocalidades];

        for (int i = 0; i < numLocalidades; i++) {
            dist[i] = INF;
            prev[i] = -1;
        }
        dist[origen] = 0;

        for (int i = 0; i < numLocalidades; i++) {
            int u = minDistancia(dist, vis);
            if (u == -1) break;
            vis[u] = true;

            for (Conexion c : adj[u]) {
                if (!vis[c.destino] && c.estaActiva()) {
                    int nuevaDist = dist[u] + c.peso;
                    if (nuevaDist < dist[c.destino]) {
                        dist[c.destino] = nuevaDist;
                        prev[c.destino] = u;
                    }
                }
            }
        }
        return dist; // dist[i] = distancia mínima desde 'origen' hasta i
    }

    // =========================================================
    //  FLOYD-WARSHALL — todas las distancias entre pares
    //  Uso epidémico: calcular qué tan "cerca" está cada localidad
    //  de cualquier foco activo
    // =========================================================
    public int[][] floydWarshall() {
        final int INF = 999999;
        int[][] dist = new int[numLocalidades][numLocalidades];

        // Inicializar con INF
        for (int i = 0; i < numLocalidades; i++) {
            for (int j = 0; j < numLocalidades; j++) {
                dist[i][j] = (i == j) ? 0 : INF;
            }
        }
        // Llenar con las aristas activas
        for (int u = 0; u < numLocalidades; u++) {
            for (Conexion c : adj[u]) {
                if (c.estaActiva()) {
                    dist[u][c.destino] = Math.min(dist[u][c.destino], c.peso);
                }
            }
        }
        // Floyd-Warshall
        for (int k = 0; k < numLocalidades; k++) {
            for (int i = 0; i < numLocalidades; i++) {
                for (int j = 0; j < numLocalidades; j++) {
                    if (dist[i][k] != INF && dist[k][j] != INF) {
                        dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
                    }
                }
            }
        }
        return dist;
    }

    // =========================================================
    //  Auxiliares internos (iguales a tu código original)
    // =========================================================
    private int minDistancia(int[] dist, boolean[] visitado) {
        int min = Integer.MAX_VALUE, idx = -1;
        for (int i = 0; i < numLocalidades; i++) {
            if (!visitado[i] && dist[i] < min) {
                min = dist[i];
                idx = i;
            }
        }
        return idx;
    }

    private int find(int[] padre, int i) {
        if (padre[i] != i) padre[i] = find(padre, padre[i]);
        return padre[i];
    }

    private void union(int[] padre, int[] rank, int x, int y) {
        if (rank[x] < rank[y])       padre[x] = y;
        else if (rank[x] > rank[y])  padre[y] = x;
        else { padre[y] = x; rank[x]++; }
    }

    public void imprimirGrafo() {
        for (int v = 0; v < numLocalidades; v++) {
            System.out.print("\n" + localidades[v].nombre + ": ");
            for (Conexion c : adj[v]) {
                System.out.print("-> " + localidades[c.destino].nombre +
                        "(w=" + c.peso + ") ");
            }
        }
    }
}
