package modelo;

// Arista del grafo (canal de movilidad entre dos localidades)
public class Conexion {

    public final int    origen;
    public final int    destino;
    public final int    peso;
    public final double tasaMovilidad;
    public final int    costoCorte;
    public boolean      cortada;

    public Conexion(int origen, int destino, int peso,
                    double tasaMovilidad, int costoCorte) {
        this.origen        = origen;
        this.destino       = destino;
        this.peso          = peso;
        this.tasaMovilidad = tasaMovilidad;
        this.costoCorte    = costoCorte;
        this.cortada       = false;
    }

    public boolean estaActiva() {
        return !cortada;
    }

    @Override
    public String toString() {
        return String.format("%d <-> %d (peso=%d, movilidad=%.2f, corte=$%d)%s",
                origen, destino, peso, tasaMovilidad, costoCorte,
                cortada ? " [CORTADA]" : "");
    }
}
