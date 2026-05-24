package modelo;

// Nodo del grafo
public class Localidad {

    public final int id;
    public String    nombre;
    public int       poblacion;
    public int       infectados;

    public Localidad(int id, String nombre, int poblacion) {
        this.id         = id;
        this.nombre     = nombre;
        this.poblacion  = Math.max(1, poblacion);
        this.infectados = 0;
    }

    public void semillaInicial(int n) {
        this.infectados = Math.min(Math.max(0, n), poblacion);
    }

    public void actualizar(String nombre, int poblacion, int infectados) {
        this.nombre     = nombre;
        this.poblacion  = Math.max(1, poblacion);
        this.infectados = Math.min(Math.max(0, infectados), this.poblacion);
    }

    public double fraccionInfectada() {
        return (double) infectados / poblacion;
    }

    public boolean esFoco() {
        return infectados > 0;
    }

    @Override
    public String toString() {
        return String.format("[%d] %-15s infectados: %d / %d (%.1f%%)",
                id, nombre, infectados, poblacion, fraccionInfectada() * 100);
    }
}
