package proyecto_final.eda;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class Grafo {
    private int V;//Cantidad de Vertices
    private int A;//Cantidad de Aristas
    private int[][] MA;//Matriz de adyacencia
    
    public Grafo(int N){
        this.V = N;
        this.A = 0;
        this.MA = new int[N][N];
        //N es la cantidad de nodos/vertices del grafo
    }
    public void addA(int N1, int N2){
        MA[N1][N2]=1;
        MA[N2][N1]=1;
        A++;
    }
    public void printMA(){
        System.out.println("MATRIZ DE ADYACENCIA");
        System.out.print("|Cols | ");
        
        for(int h=0;h<V;h++){
            if(h==V-1){
                System.out.print(h+" |");
            }
            else{
            System.out.print(h+" ");

            }
        }
        System.out.println("");
        for(int j=0;j<(9+(2*V));j++){
            System.out.print("=");
        }
        System.out.println("");
        for(int v=0; v<V;v++){
            System.out.print("|Row "+v+"| ");
            for(int w=0;w<V;w++){
                if(w!=V-1){
                    System.out.print(MA[v][w]+" ");
                }
                else{
                    System.out.print(MA[v][w]+" |");
                }
            }
            System.out.println("");
        }
    }
    public void bfs(int N){
        boolean[] visited = new boolean[V];
        Queue<Integer> q = new LinkedList<>();
        visited[N]=true;
        q.offer(N);
        System.out.print("BFS: ");
        while(!q.isEmpty()){
            int u=q.poll();
            System.out.print(u+" ");
            for(int v=0;v<V;v++){
                if(MA[u][v]==1 && !visited[v]){
                    visited[v]=true;
                    q.offer(v);
                }
                
            }
        }
        System.out.println("");
    }
    public void dfs(int N){
        boolean[] visited = new boolean[V];
        Stack<Integer> q = new Stack<>();
        q.push(N);
        System.out.print("DFS: ");
        while(!q.isEmpty()){
            int u=q.pop();
            if(!visited[u]){
                visited[u]=true;
                System.out.print(u+" ");
                for(int v=0;v<V;v++){
                    if(MA[u][v]==1 && !visited[v]){
                        q.push(v);
                    }
                }
            }
        }
        System.out.println("");
    }
    
}
