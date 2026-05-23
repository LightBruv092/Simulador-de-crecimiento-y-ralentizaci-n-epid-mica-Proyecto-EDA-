package modelo;

/**
 * ARISTA del grafo.
 * Representa un canal de movilidad entre dos localidades:
 * puede ser una carretera, ruta aérea, río navegable, etc.
 *
 * El 'peso' tiene dos lecturas según el contexto:
 *   - En Dijkstra/Floyd: es el costo de viaje (tiempo, distancia)
 *   - En Kruskal/Prim:   es el costo económico de cortar la conexión
 *
 * 'tasaMovilidad' es cuánta gente viaja por esta conexión cada tick
 * → a mayor movilidad, más rápido se propaga la epidemia.
 */
public class Conexion {

    public final int origen;
    public final int destino;
    public final int peso;              // costo de la conexión (distancia/tiempo)
    public final double tasaMovilidad;  // personas que viajan por tick (0.0 – 1.0)
    public final int costoCorte;        // presupuesto que cuesta eliminar esta arista

    public boolean cortada;             // true = conexión eliminada por el optimizador

    /**
     * @param origen         índice de la localidad de origen
     * @param destino        índice de la localidad de destino
     * @param peso           peso para los algoritmos de grafo
     * @param tasaMovilidad  fracción de población que viaja (0.0–1.0)
     * @param costoCorte     costo económico para cortar esta conexión
     */
    public Conexion(int origen, int destino, int peso,
                    double tasaMovilidad, int costoCorte) {
        this.origen        = origen;
        this.destino       = destino;
        this.peso          = peso;
        this.tasaMovilidad = tasaMovilidad;
        this.costoCorte    = costoCorte;
        this.cortada       = false;
    }

    /** ¿Está activa (no cortada)? */
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
