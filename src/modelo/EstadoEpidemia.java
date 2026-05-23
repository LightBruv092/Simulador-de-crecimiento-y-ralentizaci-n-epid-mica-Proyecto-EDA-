package modelo;

/**
 * SNAPSHOT del estado global en un tick.
 */
public class EstadoEpidemia {

    public final int tick;
    public final int[] infectados;     // infectados[i] = infectados en localidad i
    public final int[] poblaciones;
    public final int totalInfectados;

    public EstadoEpidemia(int tick, GrafoEpidemia grafo) {
        this.tick = tick;
        int n = grafo.getNumLocalidades();
        infectados  = new int[n];
        poblaciones = new int[n];
        int total   = 0;
        for (int i = 0; i < n; i++) {
            Localidad l    = grafo.getLocalidad(i);
            infectados[i]  = l.infectados;
            poblaciones[i] = l.poblacion;
            total         += l.infectados;
        }
        this.totalInfectados = total;
    }

    @Override
    public String toString() {
        return String.format("Tick %3d | Total infectados: %d", tick, totalInfectados);
    }
}
