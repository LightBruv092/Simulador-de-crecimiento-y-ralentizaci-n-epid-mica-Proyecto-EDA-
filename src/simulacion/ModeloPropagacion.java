package simulacion;

import modelo.Conexion;
import modelo.GrafoEpidemia;
import modelo.Localidad;

// Regla de contagio con recuperación gradual
public class ModeloPropagacion {

    private static final double TASA_INTERNA        = 0.01;
    private static final double TASA_RECUPERACION   = 0.008;
    private static final double UMBRAL_RECUPERACION = 0.005;

    /**
     * Devuelve delta[i] = cambio neto en infectados.
     * Positivo = nuevos contagios. Negativo = recuperación.
     */
    public int[] calcularDeltas(GrafoEpidemia grafo) {
        int n       = grafo.getNumLocalidades();
        int[] delta = new int[n];

        for (int i = 0; i < n; i++) {
            Localidad l   = grafo.getLocalidad(i);
            int       sanos = l.poblacion - l.infectados;

            // Presión interna (solo si ya hay infectados en esta localidad)
            double presionInterna = l.fraccionInfectada() * TASA_INTERNA;

            // Presión vecinal: vecinos infectados contagian a esta localidad
            double presionVecinal = 0.0;
            for (Conexion c : grafo.getAdj(i)) {
                if (!c.estaActiva()) continue;
                presionVecinal += grafo.getLocalidad(c.destino).fraccionInfectada()
                                  * c.tasaMovilidad;
            }

            double presionTotal = presionInterna + presionVecinal;

            if (presionTotal >= UMBRAL_RECUPERACION) {
                // La enfermedad avanza: nuevos contagios en esta localidad
                int nuevos = (int)(sanos * presionTotal);
                if (nuevos == 0 && presionTotal > 0.05) nuevos = 1;
                delta[i] = Math.min(nuevos, sanos);
            } else if (l.infectados > 0) {
                // Sin presión suficiente y hay infectados: recuperación gradual
                int recuperados = Math.max(1, (int)(l.infectados * TASA_RECUPERACION));
                delta[i] = -Math.min(recuperados, l.infectados);
            }
            // Si infectados == 0 y presión < umbral: delta permanece 0 (nodo sano sin presión)
        }

        return delta;
    }
}
