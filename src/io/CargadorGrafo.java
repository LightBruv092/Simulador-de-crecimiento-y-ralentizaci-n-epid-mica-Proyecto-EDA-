package io;

import modelo.Conexion;
import modelo.GrafoEpidemia;
import modelo.Localidad;

public class CargadorGrafo {

    public static GrafoEpidemia cargarEjemplo() {
        GrafoEpidemia g = new GrafoEpidemia(5);

        // Localidades con población real (simplificada)
        g.agregarLocalidad(new Localidad(0, "Bogotá",       1000));
        g.agregarLocalidad(new Localidad(1, "Medellín",      600));
        g.agregarLocalidad(new Localidad(2, "Cali",          500));
        g.agregarLocalidad(new Localidad(3, "Barranquilla",  400));
        g.agregarLocalidad(new Localidad(4, "Bucaramanga",   300));

        // Conexiones: (origen, destino, peso, movilidad, costoCorte)
        // movilidad = fracción de población que "viaja" por tick
        g.agregarConexion(new Conexion(0, 1, 4, 0.30, 200));
        g.agregarConexion(new Conexion(0, 2, 3, 0.25, 150));
        g.agregarConexion(new Conexion(1, 2, 1, 0.40, 80));
        g.agregarConexion(new Conexion(1, 3, 2, 0.20, 100));
        g.agregarConexion(new Conexion(2, 3, 4, 0.15, 90));
        g.agregarConexion(new Conexion(3, 4, 2, 0.10, 60));

        // Foco inicial: Bogotá con 10 infectados
        g.getLocalidad(0).semillaInicial(10);

        return g;
    }
}
