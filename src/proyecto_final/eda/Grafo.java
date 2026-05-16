package proyecto_final.eda;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class Grafo {
    private
            LinkedList<Arista>[]adj;
            int vertice;
            int arista;

    public Grafo(int nodos) {
        this.vertice = nodos;
        this.arista = 0;
        this.adj = new LinkedList[nodos];
        for(int v=0; v<vertice; v++){
            adj[v] = new LinkedList<>();
        }
    }
    
    public void aggAristasMejorado(int u, int v, int peso){//Le añado peso para poder implementar los algoritmos TALLER3 (ya que la idea es encontrar el camino con menor peso)
    adj[u].add(new Arista(v, peso));
    adj[v].add(new Arista(u, peso)); // grafo no dirigido
    arista++;
    }
    
    public void imprimirGrafo(){
    for(int v=0; v<vertice; v++){
        System.out.print("\nNodo "+v+": ");
        for(Arista a : adj[v]){//lo cambio implementado la clase Arista
            System.out.print("-> ("+a.destino+", "+a.peso+") ");
        }
    }
    }
    
    public void bfs(int s){
    boolean[] visited = new boolean[vertice];
    Queue<Integer> q = new LinkedList<>();
    visited[s] = true;
    q.offer(s);

    while(!q.isEmpty()){
        int u = q.poll();
        System.out.print(u + " ");

        for(Arista a : adj[u]){//cambio por la clase Arista para que siga funcionando
            int v = a.destino;
            if(!visited[v]){
                visited[v] = true;
                q.offer(v);
            }
        }
    }
    }
    
    public void dfs(int s){
    boolean[] visited = new boolean[vertice];
    Stack<Integer> stack = new Stack<>();
    stack.push(s);

    while(!stack.isEmpty()){
        int u = stack.pop();

        if(!visited[u]){
            visited[u] = true;
            System.out.print(u + " ");

            for(Arista a : adj[u]){//de mismo modo que con bfs
                int v = a.destino;
                if(!visited[v]){
                    stack.push(v);
                }
            }
        }
    }
    }
    
    //TALLER 3
    public int minDist(int[] dist, boolean[] visited){//función para medir la mínima distancia entre vertices
        int min = Integer.MAX_VALUE, index=-1;
        for(int i=0;i<vertice;i++){
            if(!visited[i] && dist[i]<min){
                min = dist[i];
                index=i;
            }
        }
        return index;
    }
    
    
    private int find(int[] padre, int i){//Sin ciclos para Kruskal
    if(padre[i] != i)
        padre[i] = find(padre, padre[i]);
    return padre[i];
    }
    
    public void kruskal(){
    LinkedList<AristaKruskal> lista = new LinkedList<>();

    for(int u = 0; u < vertice; u++){// Pasar del adj a lista de aristas sin duplicados
        for(Arista a : adj[u]){
            if(u < a.destino){
                lista.add(new AristaKruskal(u, a.destino, a.peso));
            }
        }
    }
    
    lista.sort((a, b) -> a.peso - b.peso);// Ordenar por peso
    int[] padre = new int[vertice];
    int[] rank = new int[vertice];

    for(int i = 0; i < vertice; i++){
        padre[i] = i;
        rank[i] = 0;
    }

    int contador = 0;
    System.out.println("Aristas del árbol de expamsión mínima:");
    for(AristaKruskal e : lista){
        int x = find(padre, e.origen);
        int y = find(padre, e.destino);

        if(x != y){
            System.out.println(e.origen + "-" + e.destino + ", peso: " + e.peso);
            if(rank[x] < rank[y]){// unión con rank, como Kruskal evita ciclos se puede ver como un árbol que conecta las aritas de menor peso
                padre[x] = y;
            }else if(rank[x] > rank[y]){
                padre[y] = x;
            }else{
                padre[y] = x;
                rank[x]++;
            }

            contador++;

            if(contador == vertice - 1)
                break;
        }
    }
    }
    
    public void prim(){
    int[] peso = new int[vertice];
    int[] padre = new int[vertice];
    boolean[] arbolEM = new boolean[vertice];//árbol de expansión mínima

    for(int i=0;i<vertice;i++){
        peso[i]=Integer.MAX_VALUE;
        arbolEM[i]=false;
    }
    peso[0]=0;//para llevar el peso entre aristas
    padre[0]=-1;
    
    System.out.println("Aristas del árbol de expamsión mínima:");
    for(int i=0;i<vertice-1;i++){
        int u = minDist(peso, arbolEM);
        arbolEM[u]=true;

        for(Arista a: adj[u]){
            int v=a.destino;
            if(!arbolEM[v] && a.peso < peso[v]){
                padre[v]=u;
                peso[v]=a.peso;
            }
        }
    }
    for(int i=1;i<vertice;i++)
        System.out.println(padre[i]+"-"+i+", peso: "+peso[i]);
    }
    
    //Este imprimir solo es para dijtra
    private void imprimirCamino(int destino, int[] prev){// Este metodo me sirve para que Dijkstra me diga la trayectoria para obtener los pesos minimos
    if(destino == -1) return;

    imprimirCamino(prev[destino], prev);
    System.out.print(destino + " ");
    }
    
    public void dijkstra(int origen){//cacula la distancia más desde el origen hasta los otros nodos
    int INF = 999999;//para simular el infinito (cuando no tienen un camino directo)

    int[] dist = new int[vertice];
    boolean[] visitado = new boolean[vertice];
    int[] prev = new int[vertice]; // para reconstruir camino

    for(int i=0;i<vertice;i++){
        dist[i] = INF;
        visitado[i] = false;
        prev[i] = -1;
    }

    dist[origen] = 0;

    for(int i=0;i<vertice;i++){
        int u = minDist(dist, visitado);
        visitado[u] = true;

        for(Arista a : adj[u]){
            int v = a.destino;
            int peso = a.peso;

            if(!visitado[v] && dist[u] + peso < dist[v]){
                dist[v] = dist[u] + peso;
                prev[v] = u; // guarda de dónde vengo
            }
        }
    }

    for(int i=0;i<vertice;i++){
        System.out.print("\nDistancia de " + origen + " a " + i + " = " + dist[i] + " | Camino: ");
        imprimirCamino(i, prev);
    }
    }
    
    public void imprimirCaminoFloyd(int u, int v, int[][] next){//Este imprimir solo es para Floyd (a diferencia de dijkstra necesita una maztriz(la de los valores de los vertices)
    if(next[u][v] == -1){
        System.out.println("No hay camino");
        return;
    }

    System.out.print(u);
    while(u != v){
        u = next[u][v];
        System.out.print(" -> " + u);
    }
    System.out.println();
    }
    public void floydWarshall(int[][] grafo){
    int n = grafo.length;
    int INF = 999999;

    int[][] dist = new int[n][n];
    int[][] next = new int[n][n];

    for(int i=0;i<n;i++){// inicialización
        for(int j=0;j<n;j++){
            dist[i][j] = grafo[i][j];

            if(grafo[i][j] != INF && i != j)
                next[i][j] = j;
            else
                next[i][j] = -1;
        }
    }

    for(int k=0;k<n;k++){//Floyd
        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++){
                if(dist[i][k] + dist[k][j] < dist[i][j]){
                    dist[i][j] = dist[i][k] + dist[k][j];
                    next[i][j] = next[i][k];
                }
            }
        }
    }

    System.out.println("Matriz de adyacencia (pesos): ");
    for(int i=0;i<dist.length;i++){
        for(int j=0;j<dist.length;j++){
            System.out.print(dist[i][j] + "\t");
        }
        System.out.println();
    }
    
    System.out.println("\nMatriz de adyacencia (recorridos):");
    for(int i=0;i<next.length;i++){
        for(int j=0;j<next.length;j++){
            System.out.print(next[i][j] + "\t");
        }
        System.out.println();
    }
}
}
