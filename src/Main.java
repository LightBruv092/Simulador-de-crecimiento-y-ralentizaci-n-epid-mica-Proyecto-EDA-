import io.CargadorGrafo;
import io.InformeSimulacion;
import modelo.GrafoEpidemia;
import optimizador.EstrategiaCorte;
import optimizador.ModeloEconomico;
import optimizador.SolucionadorContencion;
import simulacion.ModeloPropagacion;
import simulacion.MotorEpidemia;

import java.util.List;
public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== EpiGrafo ===\n");

        GrafoEpidemia grafo = CargadorGrafo.cargarEjemplo();
        grafo.imprimirGrafo();

        System.out.println("\n\n[BFS] Alcanzables desde Bogotá:");
        grafo.bfsDesde(0).forEach(i -> System.out.print(grafo.getLocalidad(i).nombre + " "));

        System.out.println("\n\n[Dijkstra] Distancias desde Bogotá:");
        int[] dist = grafo.dijkstra(0);
        for (int i = 0; i < grafo.getNumLocalidades(); i++)
            System.out.printf("  → %-15s : %d%n", grafo.getLocalidad(i).nombre, dist[i]);

        System.out.println("\n[Kruskal] MST:");
        grafo.kruskal().forEach(c -> System.out.printf("  %s <-> %s (peso=%d)%n",
                grafo.getLocalidad(c.origen).nombre,
                grafo.getLocalidad(c.destino).nombre, c.peso));

        System.out.println("\n[Simulación] Estado inicial:");
        for (int i = 0; i < grafo.getNumLocalidades(); i++)
            System.out.println("  " + grafo.getLocalidad(i));

        MotorEpidemia motor = new MotorEpidemia(grafo, new ModeloPropagacion());
        System.out.println("\n--- Corriendo 15 ticks ---");
        motor.correrTicks(15);

        System.out.println("\n[Optimizador] Cortes con $250:");
        new SolucionadorContencion(grafo, new ModeloEconomico(1.0), 250).resolver();

        System.out.println("\n--- 10 ticks más post-cortes ---");
        motor.correrTicks(10);

        InformeSimulacion.imprimirResumen(motor.getHistorial());

        // Descomenta para UI:
         javax.swing.SwingUtilities.invokeLater(ui.EpiGrafoUI::new);
    }
}