package optimizador;

import modelo.Conexion;

/**
 * RESULTADO de cortar una conexión.
 */
public class EstrategiaCorte {

    public final Conexion conexion;
    public final int costoReal;
    public final boolean eraFrontera; // ¿conectaba infectado con sano?

    public EstrategiaCorte(Conexion conexion, int costoReal, boolean eraFrontera) {
        this.conexion    = conexion;
        this.costoReal   = costoReal;
        this.eraFrontera = eraFrontera;
    }

    @Override
    public String toString() {
        return String.format("Corte %d<->%d | $%d%s",
                conexion.origen, conexion.destino, costoReal,
                eraFrontera ? " [frontera]" : "");
    }
}
