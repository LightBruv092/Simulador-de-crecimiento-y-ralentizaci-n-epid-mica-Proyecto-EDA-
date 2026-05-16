package proyecto_final.eda;
public class Main {
    public static void main(String[] args) {
        Grafo g= new Grafo(5);
        g.aggAristasMejorado(0,1,4);//(vertice, vertice al que esta conectado, peso)
        g.aggAristasMejorado(0,2,3);
        g.aggAristasMejorado(1,2,1);
        g.aggAristasMejorado(1,3,2);
        g.aggAristasMejorado(2,3,4);
        g.aggAristasMejorado(3,4,2);
        
        System.out.println("Grafo original");
        g.imprimirGrafo();
        System.out.println("\n\nDFS");
        g.dfs(0);
        System.out.println("\n\nBFS");
        g.bfs(0);
        
        //TALLER3
        System.out.println("\n\nKruskal");
        g.kruskal();
        System.out.println("\n\nPrim");
        g.prim();
        System.out.println("\n\nDijkstra");//el origen en este caso es 0
        g.dijkstra(0);
        
        System.out.println("\n\nFloyd-Warshall");
        int INF = 999999;
        int[][] matriz = {//es la matriz del grafo g
        //   0    1    2    3    4
        { 0,   4,   3,  INF, INF}, // 0
        { 4,   0,   1,   2,  INF}, // 1
        { 3,   1,   0,   4,  INF}, // 2
        { INF, 2,   4,   0,   2  }, // 3
        { INF, INF, INF, 2,   0  }  // 4
        };
        
        g.floydWarshall(matriz);
    }
    
}
