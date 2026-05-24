package simulacion;

import modelo.EstadoEpidemia;
import modelo.GrafoEpidemia;
import modelo.Localidad;

import java.util.List;

/**
 * CICLO PRINCIPAL DE SIMULACIÓN.
 *
 * Cada tick:
 *   1. ModeloPropagacion calcula cuántos nuevos infectados hay en cada ciudad
 *   2. Se aplican todos los cambios a la vez
 *   3. Se guarda un snapshot
 */
public class MotorEpidemia {

    private final GrafoEpidemia     grafo;
    private final ModeloPropagacion modelo;
    private final RegistroHistorial historial;
    private int tickActual = 0;

    public MotorEpidemia(GrafoEpidemia grafo, ModeloPropagacion modelo) {
        this.grafo     = grafo;
        this.modelo    = modelo;
        this.historial = new RegistroHistorial();
    }

    public void tick() {
        tickActual++;

        // Calcular nuevos contagios sin modificar el grafo todavía
        int[] delta = modelo.calcularDeltas(grafo);

        // Aplicar todos los cambios a la vez
        for (int i = 0; i < grafo.getNumLocalidades(); i++) {
            Localidad l = grafo.getLocalidad(i);
            l.infectados = Math.max(0, Math.min(l.infectados + delta[i], l.poblacion));
        }

        EstadoEpidemia estado = new EstadoEpidemia(tickActual, grafo);
        historial.registrar(estado);
        System.out.println(estado);
    }

    public void correrTicks(int n) {
        for (int i = 0; i < n; i++) {
            tick();
            if (todasLlenas()) {
                System.out.println("Toda la población está infectada (tick " + tickActual + ")");
                break;
            }
        }
    }

    public boolean todasLlenas() {
        for (int i = 0; i < grafo.getNumLocalidades(); i++) {
            Localidad l = grafo.getLocalidad(i);
            if (l.infectados < l.poblacion) return false;
        }
        return true;
    }

    /** BFS desde el foco más grande → qué ciudades son alcanzables */
    public List<Integer> zonasEnRiesgo() {
        int focoMax = 0;
        for (int i = 0; i < grafo.getNumLocalidades(); i++) {
            if (grafo.getLocalidad(i).infectados > grafo.getLocalidad(focoMax).infectados)
                focoMax = i;
        }
        return grafo.bfsDesde(focoMax);
    }

    /** Dijkstra desde el foco más grande → distancia a cada ciudad */
    public int[] distanciasDesdeFoco() {
        int focoMax = 0;
        for (int i = 0; i < grafo.getNumLocalidades(); i++) {
            if (grafo.getLocalidad(i).infectados > grafo.getLocalidad(focoMax).infectados)
                focoMax = i;
        }
        return grafo.dijkstra(focoMax);
    }

    public RegistroHistorial getHistorial() { return historial; }
    public int getTickActual()              { return tickActual; }
}
