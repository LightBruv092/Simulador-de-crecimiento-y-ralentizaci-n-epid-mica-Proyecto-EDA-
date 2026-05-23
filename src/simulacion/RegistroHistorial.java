package simulacion;

import modelo.EstadoEpidemia;
import java.util.ArrayList;
import java.util.List;

public class RegistroHistorial {
    private final List<EstadoEpidemia> historial = new ArrayList<>();

    public void registrar(EstadoEpidemia e) { historial.add(e); }

    public EstadoEpidemia ultimoEstado() {
        return historial.isEmpty() ? null : historial.get(historial.size() - 1);
    }

    public List<EstadoEpidemia> getTodos() { return historial; }

    public int peakInfectados() {
        return historial.stream().mapToInt(e -> e.totalInfectados).max().orElse(0);
    }
}
