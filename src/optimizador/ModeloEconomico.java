package optimizador;

import modelo.Conexion;

/**
 * COSTO ECONÓMICO DE CORTAR CADA CONEXIÓN.
 * Puede ser tan simple o complejo como necesites:
 * - Simple: usar el campo costoCorte de la propia Conexion
 * - Complejo: multiplicar por factores (importancia comercial, población afectada...)
 */
public class ModeloEconomico {

    private double factorMultiplicador; // 1.0 = sin ajuste

    public ModeloEconomico(double factorMultiplicador) {
        this.factorMultiplicador = factorMultiplicador;
    }

    public int costoDeCorte(Conexion c) {
        return (int)(c.costoCorte * factorMultiplicador);
    }

    public void setFactor(double f) { this.factorMultiplicador = f; }
}
