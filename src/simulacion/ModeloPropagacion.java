package simulacion;

import modelo.Conexion;
import modelo.GrafoEpidemia;
import modelo.Localidad;

/**
 * REGLA DE CONTAGIO proporcional.
 *
 * Cada tick, para cada localidad:
 *
 *   nuevos_contagios = sanos × (presion_interna + presion_vecinal)
 *
 * PRESIÓN INTERNA (contagio dentro de la misma ciudad):
 *   presion_interna = fraccion_infectada_local × TASA_INTERNA
 *   → cuantos más infectados hay en la ciudad, más rápido contagian
 *     a los sanos que conviven con ellos.
 *   → si hay 1% infectado la presión es baja; si hay 60% es alta.
 *
 * PRESIÓN VECINAL (viajeros infectados que llegan de otras ciudades):
 *   presion_vecinal = suma(fraccion_infectada_vecino × movilidad_conexion)
 *   → depende de cuántos infectados tienen las ciudades vecinas
 *     y de qué tan transitada es la conexión.
 *
 * TASA_INTERNA: qué tan contagiosa es la enfermedad dentro de una ciudad.
 * Con 0.2: al 50% infectados, la ciudad produce 0.1 nuevos contagios
 * por sano por tick — razonable para una enfermedad moderada.
 */
public class ModeloPropagacion {

    // Ajusta este valor para controlar la velocidad del contagio interno.
    // 0.05 = lento,  0.2 = moderado,  0.5 = muy rápido
    private static final double TASA_INTERNA = 0.01;

    /**
     * Calcula cuántos nuevos infectados habrá en cada localidad el próximo tick.
     * NO modifica el grafo todavía — devuelve los deltas para aplicarlos todos a la vez.
     *
     * @return delta[i] = nuevos infectados en localidad i este tick
     */
    public int[] calcularDeltas(GrafoEpidemia grafo) {
        int n       = grafo.getNumLocalidades();
        int[] delta = new int[n];

        for (int i = 0; i < n; i++) {
            Localidad l = grafo.getLocalidad(i);
            int sanos   = l.poblacion - l.infectados;
            if (sanos <= 0) continue;

            // --- Presión interna ---
            // infectados de la propia ciudad contagian a sus sanos convivientes
            double presionInterna = l.fraccionInfectada() * TASA_INTERNA;

            // --- Presión vecinal ---
            // infectados de ciudades vecinas que viajan aquí
            double presionVecinal = 0.0;
            for (Conexion c : grafo.getAdj(i)) {
                if (!c.estaActiva()) continue;
                Localidad vecino = grafo.getLocalidad(c.destino);
                presionVecinal += vecino.fraccionInfectada() * c.tasaMovilidad;
            }

            double presionTotal = presionInterna + presionVecinal;

            int nuevos = (int)(sanos * presionTotal);
            if (nuevos == 0 && presionTotal > 0.05) nuevos = 1;
            delta[i] = Math.min(nuevos, sanos);
        }

        return delta;
    }
}
