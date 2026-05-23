package io;

import modelo.EstadoEpidemia;
import simulacion.RegistroHistorial;

public class InformeSimulacion {
    public static void imprimirResumen(RegistroHistorial historial) {
        System.out.println("\n=== RESUMEN ===");
        historial.getTodos().forEach(System.out::println);
        System.out.println("Pico infectados: " + historial.peakInfectados());
    }
}
