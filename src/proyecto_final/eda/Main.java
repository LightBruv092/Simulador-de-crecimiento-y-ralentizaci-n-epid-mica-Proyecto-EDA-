package proyecto_final.eda;
public class Main {
    public static void main(String[] args) {
        Grafo g= new Grafo(5);
        g.addA(0,1);
        g.addA(1,2);
        g.addA(2,3);
        g.addA(3,4);
        g.addA(4,0);
        g.printMA();
        g.bfs(0);
        g.dfs(0);
    }
    
}
