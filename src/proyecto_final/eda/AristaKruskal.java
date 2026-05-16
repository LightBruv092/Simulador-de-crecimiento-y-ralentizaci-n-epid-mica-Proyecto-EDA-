
package proyecto_final.eda;

public class AristaKruskal {
    int origen;
    int destino;
    int peso;
    
    AristaKruskal(int o,int d,int p){//para Krukal necesito saber de donde viene la conexión del vertice(origen) para evitar los ciclos
        this.origen=o;
        this.destino=d;
        this.peso=p;
    }
    
    public int compareTo(AristaKruskal peso2){//compara los peso de las arista para encontrar el menor
        return this.peso-peso2.peso;
    }
}
