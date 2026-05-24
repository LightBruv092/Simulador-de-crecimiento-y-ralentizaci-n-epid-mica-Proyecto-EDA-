package optimizador;

import modelo.Conexion;
import modelo.GrafoEpidemia;
import modelo.Localidad;

import java.util.ArrayList;
import java.util.List;

/**
 * SELECCIÓN DE CORTES.
 *
 * Prioridad de corte:
 *   peligro = fraccion_infectada_origen × movilidad / costo
 *
 * Las aristas con mayor peligro se cortan primero dentro del presupuesto.
 * Las aristas del MST (Kruskal) se evitan para no desconectar el grafo.
 */
public class SolucionadorContencion {

    private final GrafoEpidemia   grafo;
    private final ModeloEconomico economico;
    private int presupuesto;

    public SolucionadorContencion(GrafoEpidemia grafo,
                                   ModeloEconomico economico,
                                   int presupuesto) {
        this.grafo       = grafo;
        this.economico   = economico;
        this.presupuesto = presupuesto;
    }

    /** Qué tan urgente es cortar esta arista */
    private double peligro(Conexion c) {
        double fracOrigen  = grafo.getLocalidad(c.origen).fraccionInfectada();
        double fracDestino = grafo.getLocalidad(c.destino).fraccionInfectada();
        double fracMax     = Math.max(fracOrigen, fracDestino);
        return (fracMax * c.tasaMovilidad) / Math.max(1, economico.costoDeCorte(c));
    }

    public List<EstrategiaCorte> resolver() {
        List<Conexion> mst        = grafo.kruskal();
        List<Conexion> candidatos = aristasFueraMST(mst);

        // Ordenar por peligro descendente
        candidatos.sort((a, b) -> Double.compare(peligro(b), peligro(a)));

        List<EstrategiaCorte> cortes = new ArrayList<>();
        int restante = presupuesto;

        for (Conexion c : candidatos) {
            int costo = economico.costoDeCorte(c);
            if (costo <= restante) {
                // Marcar ambas direcciones del grafo como cortadas
                c.cortada = true;
                cortarDireccionInversa(c);

                restante -= costo;
                cortes.add(new EstrategiaCorte(c, costo, peligro(c) > 0));
                System.out.printf("Cortando: %s <-> %s ($%d, peligro=%.3f)%n",
                        grafo.getLocalidad(c.origen).nombre,
                        grafo.getLocalidad(c.destino).nombre,
                        costo, peligro(c));
            }
        }
        System.out.printf("Presupuesto usado: $%d / $%d%n",
                presupuesto - restante, presupuesto);
        return cortes;
    }

    /**
     * Marca como cortada la conexión inversa (destino → origen) en la
     * lista de adyacencia, para que el corte sea efectivo en ambas
     * direcciones del grafo bidireccional.
     */
    private void cortarDireccionInversa(Conexion c) {
        for (Conexion rev : grafo.getAdj(c.destino)) {
            if (rev.destino == c.origen) {
                rev.cortada = true;
                break;
            }
        }
    }

    private List<Conexion> aristasFueraMST(List<Conexion> mst) {
        List<Conexion> fuera = new ArrayList<>();
        for (int u = 0; u < grafo.getNumLocalidades(); u++) {
            for (Conexion c : grafo.getAdj(u)) {
                if (u < c.destino && c.estaActiva()) {
                    boolean enMST = mst.stream().anyMatch(m ->
                            (m.origen == c.origen && m.destino == c.destino) ||
                            (m.origen == c.destino && m.destino == c.origen));
                    if (!enMST) fuera.add(c);
                }
            }
        }
        return fuera;
    }

    public void setPresupuesto(int p) { this.presupuesto = p; }
}
