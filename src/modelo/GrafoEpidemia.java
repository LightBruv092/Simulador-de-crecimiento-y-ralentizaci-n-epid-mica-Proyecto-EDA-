package modelo;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

// Red geográfica con algoritmos de grafo y capa epidémica
public class GrafoEpidemia {

    private int                   numLocalidades;
    private LinkedList<Conexion>[] adj;
    private Localidad[]            localidades;

    @SuppressWarnings("unchecked")
    public GrafoEpidemia(int n) {
        this.numLocalidades = n;
        this.adj            = new LinkedList[n];
        this.localidades    = new Localidad[n];
        for (int i = 0; i < n; i++) adj[i] = new LinkedList<>();
    }

    // --- Construcción ---

    public void agregarLocalidad(Localidad l) {
        localidades[l.id] = l;
    }

    // Conexión bidireccional
    public void agregarConexion(Conexion c) {
        adj[c.origen].add(c);
        Conexion inversa = new Conexion(c.destino, c.origen,
                c.peso, c.tasaMovilidad, c.costoCorte);
        inversa.cortada = c.cortada;   // propagar el estado de corte
        adj[c.destino].add(inversa);
    }

    public Localidad            getLocalidad(int i)    { return localidades[i]; }
    public int                  getNumLocalidades()    { return numLocalidades; }
    public LinkedList<Conexion> getAdj(int v)          { return adj[v]; }

    // --- BFS: localidades alcanzables desde un foco ---
    public List<Integer> bfsDesde(int origen) {
        boolean[] visitado = new boolean[numLocalidades];
        Queue<Integer> cola = new LinkedList<>();
        List<Integer> orden = new ArrayList<>();
        visitado[origen] = true;
        cola.offer(origen);
        while (!cola.isEmpty()) {
            int u = cola.poll();
            orden.add(u);
            for (Conexion c : adj[u])
                if (!visitado[c.destino] && c.estaActiva()) {
                    visitado[c.destino] = true;
                    cola.offer(c.destino);
                }
        }
        return orden;
    }

    // --- DFS: cadenas de transmisión ---
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
                for (Conexion c : adj[u])
                    if (!visitado[c.destino] && c.estaActiva()) pila.push(c.destino);
            }
        }
        return orden;
    }

    // --- Kruskal: MST por peso ---
    public List<Conexion> kruskal() {
        List<Conexion> aristas = new ArrayList<>();
        for (int u = 0; u < numLocalidades; u++)
            for (Conexion c : adj[u])
                if (u < c.destino && c.estaActiva()) aristas.add(c);

        aristas.sort((a, b) -> a.peso - b.peso);

        int[] padre = new int[numLocalidades];
        int[] rank  = new int[numLocalidades];
        for (int i = 0; i < numLocalidades; i++) padre[i] = i;

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

    // --- Prim: MST alternativo ---
    public int[] prim() {
        final int INF = Integer.MAX_VALUE;
        int[] peso   = new int[numLocalidades];
        int[] padre  = new int[numLocalidades];
        boolean[] en = new boolean[numLocalidades];
        for (int i = 0; i < numLocalidades; i++) { peso[i] = INF; padre[i] = -1; }
        peso[0] = 0;
        for (int i = 0; i < numLocalidades - 1; i++) {
            int u = minDistancia(peso, en);
            if (u == -1) break;
            en[u] = true;
            for (Conexion c : adj[u])
                if (!en[c.destino] && c.estaActiva() && c.peso < peso[c.destino]) {
                    padre[c.destino] = u;
                    peso[c.destino]  = c.peso;
                }
        }
        return padre;
    }

    // --- Dijkstra: camino más rápido de propagación ---
    public int[] dijkstra(int origen) {
        final int INF = 999999;
        int[]     dist = new int[numLocalidades];
        boolean[] vis  = new boolean[numLocalidades];
        Arrays.fill(dist, INF);
        dist[origen] = 0;
        for (int i = 0; i < numLocalidades; i++) {
            int u = minDistancia(dist, vis);
            if (u == -1) break;
            vis[u] = true;
            for (Conexion c : adj[u])
                if (!vis[c.destino] && c.estaActiva()) {
                    int nd = dist[u] + c.peso;
                    if (nd < dist[c.destino]) dist[c.destino] = nd;
                }
        }
        return dist;
    }

    // --- Floyd-Warshall: distancias entre todos los pares ---
    public int[][] floydWarshall() {
        final int INF = 999999;
        int[][] dist = new int[numLocalidades][numLocalidades];
        for (int i = 0; i < numLocalidades; i++)
            for (int j = 0; j < numLocalidades; j++)
                dist[i][j] = (i == j) ? 0 : INF;
        for (int u = 0; u < numLocalidades; u++)
            for (Conexion c : adj[u])
                if (c.estaActiva()) dist[u][c.destino] = Math.min(dist[u][c.destino], c.peso);
        for (int k = 0; k < numLocalidades; k++)
            for (int i = 0; i < numLocalidades; i++)
                for (int j = 0; j < numLocalidades; j++)
                    if (dist[i][k] != INF && dist[k][j] != INF)
                        dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
        return dist;
    }

    // --- Modificación dinámica ---

    // Expande los arrays internos y añade la localidad al final
    @SuppressWarnings("unchecked")
    public void agregarLocalidadNueva(Localidad l) {
        int n        = numLocalidades + 1;
        localidades  = Arrays.copyOf(localidades, n);
        adj          = Arrays.copyOf(adj, n);
        localidades[n - 1] = l;
        adj[n - 1]         = new LinkedList<>();
        numLocalidades     = n;
    }

    // Sustituye los datos de una localidad sin tocar sus conexiones
    public void reemplazarLocalidad(int id, Localidad nueva) {
        Localidad actual = localidades[id];
        if (actual == null) {
            localidades[id] = nueva;
            return;
        }
        actual.actualizar(nueva.nombre, nueva.poblacion, nueva.infectados);
    }

    // Elimina la conexión bidireccional entre u y v
    public void eliminarConexion(int u, int v) {
        adj[u].removeIf(c -> c.destino == v);
        adj[v].removeIf(c -> c.destino == u);
    }

    // Elimina todas las conexiones de un nodo en ambas direcciones
    public void eliminarTodasConexiones(int id) {
        for (Conexion c : new ArrayList<>(adj[id]))
            adj[c.destino].removeIf(x -> x.destino == id);
        adj[id].clear();
    }

    // Copia profunda del grafo, preservando infectados y cortes
    @SuppressWarnings("unchecked")
    public GrafoEpidemia clonarEstructura() {
        GrafoEpidemia clon = new GrafoEpidemia(numLocalidades);
        for (int i = 0; i < numLocalidades; i++) {
            Localidad o = localidades[i];
            if (o == null) continue;
            Localidad c = new Localidad(o.id, o.nombre, o.poblacion);
            c.semillaInicial(o.infectados);
            clon.localidades[i] = c;
        }
        for (int i = 0; i < numLocalidades; i++)
            for (Conexion c : adj[i])
                if (c.origen < c.destino) {
                    Conexion copia = new Conexion(
                            c.origen, c.destino, c.peso, c.tasaMovilidad, c.costoCorte);
                    copia.cortada = c.cortada;
                    clon.agregarConexion(copia);
                }
        return clon;
    }

    public void imprimirGrafo() {
        for (int v = 0; v < numLocalidades; v++) {
            if (localidades[v] == null) continue;
            System.out.print("\n" + localidades[v].nombre + ": ");
            for (Conexion c : adj[v])
                if (localidades[c.destino] != null)
                    System.out.print("-> " + localidades[c.destino].nombre + "(w=" + c.peso + ") ");
        }
    }

    // --- Auxiliares internos ---

    private int minDistancia(int[] dist, boolean[] visitado) {
        int min = Integer.MAX_VALUE, idx = -1;
        for (int i = 0; i < numLocalidades; i++)
            if (!visitado[i] && dist[i] < min) { min = dist[i]; idx = i; }
        return idx;
    }

    private int find(int[] padre, int i) {
        if (padre[i] != i) padre[i] = find(padre, padre[i]);
        return padre[i];
    }

    private void union(int[] padre, int[] rank, int x, int y) {
        if      (rank[x] < rank[y]) padre[x] = y;
        else if (rank[x] > rank[y]) padre[y] = x;
        else { padre[y] = x; rank[x]++; }
    }
}
