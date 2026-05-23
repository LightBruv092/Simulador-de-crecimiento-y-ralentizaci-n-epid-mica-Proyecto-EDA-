package modelo;

/**
 * NODO del grafo.
 * Cada localidad tiene una población total y un contador de infectados.
 *
 * Estado:
 *   infectados = 0                  → sana
 *   0 < infectados < poblacion      → en riesgo / propagándose
 *   infectados == poblacion         → totalmente infectada
 */
public class Localidad {

    public final int id;
    public final String nombre;
    public final int poblacion;

    public int infectados; // cuántas personas están enfermas ahora mismo

    public Localidad(int id, String nombre, int poblacion) {
        this.id        = id;
        this.nombre    = nombre;
        this.poblacion = poblacion;
        this.infectados = 0;
    }

    /** Foco inicial: empieza con N infectados */
    public void semillaInicial(int n) {
        this.infectados = Math.min(n, poblacion);
    }

    /** Fracción de la población infectada (0.0 – 1.0) */
    public double fraccionInfectada() {
        return (double) infectados / poblacion;
    }

    /** ¿Tiene al menos un infectado? */
    public boolean esFoco() {
        return infectados > 0;
    }

    @Override
    public String toString() {
        return String.format("[%d] %-15s infectados: %d / %d (%.1f%%)",
                id, nombre, infectados, poblacion, fraccionInfectada() * 100);
    }
}
