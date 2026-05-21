# 🦠 Simulador de Crecimiento y Ralentización Epidémica

> Simulación realista de epidemias sobre redes de localidades modeladas como grafos,  
> con motor de contención óptima bajo restricciones económicas. **Java puro, sin dependencias externas.**

---

## 📌 Descripción General

**EpiGrafo** modela la propagación de una epidemia a través de concentraciones de población (**localidades**) interconectadas. Cada localidad posee propiedades demográficas y sanitarias que afectan directamente la dinámica de contagio. El sistema incorpora un motor de optimización capaz de decidir qué conexiones cortar para contener la epidemia de la forma más eficiente, respetando un presupuesto económico limitado.

Todo el sistema está implementado en **Java estándar (JDK 17+)** sin librerías externas, salvo las indicadas al final.

---

## 🗺️ Arquitectura del Sistema

```
EpiGrafo
├── modelo/
│   ├── Localidad.java           # Nodo del grafo: concentración de población
│   ├── Conexion.java            # Arista: canal de movilidad entre localidades
│   ├── Grafo.java               # Red geográfica completa
│   └── EstadoEpidemia.java      # Snapshot del estado global en un tick
├── simulacion/
│   ├── MotorEpidemia.java       # Ciclo principal de simulación SEIR
│   ├── ModeloPropagacion.java   # Cálculo de tasas dinámicas β, γ, μ
│   └── RegistroHistorial.java   # Almacena snapshots por tick
├── optimizador/
│   ├── SolucionadorContencion.java  # Selección óptima de cortes
│   ├── ModeloEconomico.java         # Coste de cortar cada conexión
│   └── EstrategiaCorte.java         # Resultado: conjunto de conexiones a eliminar
└── io/
    ├── CargadorGrafo.java       # Lectura desde JSON/CSV
    └── InformeSimulacion.java   # Exportación de resultados
```

---

## 🏘️ El Nodo: La Localidad

Una **localidad** no es simplemente una ciudad. Es una concentración de población con propiedades que influyen de forma realista en cómo se origina, amplifica y resiste la epidemia.

```java
public class Localidad {

    // — Identificación —
    private final String id;
    private final String nombre;

    // — Demografía —
    private final long   poblacion;             // Habitantes totales
    private final double densidadPorKm2;        // Densidad: amplifica β localmente
    private final double edadMediana;           // Edad mediana: afecta γ y mortalidad
    private final double proporcionMayores;     // % mayores de 65: grupo de riesgo
    private final double proporcionJovenes;     // % menores de 15: baja letalidad, alta movilidad

    // — Sanidad —
    private final int    camasHospitalarias;    // Camas por 1000 hab: limita γ bajo saturación
    private final double tasaInmunidadBase;     // Inmunidad preexistente (0.0–1.0)
    private final double tasaVacunacion;        // Fracción vacunada al inicio

    // — Estado epidémico SEIR —
    private double susceptibles;
    private double expuestos;
    private double infectados;
    private double recuperados;
    private double fallecidos;                  // Acumulado de fallecidos

    // — Indicadores derivados —
    private boolean sistemaSanitarioSaturado;   // true si I > capacidad hospitalaria
}
```

### ¿Por qué cada propiedad?

| Propiedad | Efecto en el modelo |
|-----------|---------------------|
| `densidadPorKm2` | A mayor densidad → mayor `β` local (más contactos por unidad de tiempo) |
| `edadMediana` / `proporcionMayores` | Aumenta la tasa de mortalidad `μ` y reduce `γ` (recuperación más lenta) |
| `camasHospitalarias` | Cuando `I` supera la capacidad → `γ` se degrada y `μ` escala (sistema saturado) |
| `tasaInmunidadBase` | Reduce la proporción efectiva de `S` desde el primer tick |
| `tasaVacunacion` | Mueve población directamente de `S` a `R` antes de iniciar la simulación |
| `proporcionJovenes` | Alta movilidad → mayor contribución al flujo entre localidades |

---

## 🔗 La Arista: La Conexión

Una conexión entre dos localidades representa un **canal de movilidad** con propiedades que determinan cuánta enfermedad fluye a través de ella.

```java
public class Conexion {

    private final Localidad origen;
    private final Localidad destino;

    // — Movilidad —
    private final int          viajerosdiarios;    // Personas que cruzan esta conexión por día
    private final TipoConexion tipo;               // AEREA, FERROVIARIA, TERRESTRE, MARITIMA

    // — Propagación —
    private double pesoPropagacion;                // Peso calculado: función de viajerosdiarios + tipo

    // — Economía del corte —
    private final double costeCorte;               // Coste económico de cerrar esta conexión
    private final double costeSocial;              // Coste social (desabastecimiento, empleo)
    private boolean      activa;                   // false = conexión cortada
}
```

```java
public enum TipoConexion {
    //                multiplicadorBase  (refleja promiscuidad de contagio en el medio)
    AEREA        (1.20),   // Espacios cerrados, alta densidad puntual
    FERROVIARIA  (1.10),   // Vagones cerrados, contacto prolongado
    TERRESTRE    (1.00),   // Referencia base
    MARITIMA     (0.70);   // Menor flujo, viajes más largos

    private final double multiplicadorBase;
}
```

---

## 🧬 Módulo 1 — Motor de Simulación SEIR Realista

### Ecuaciones del Modelo

Se usa el modelo **SEIR discreto** con extensiones de realismo. En cada tick (= 1 día):

```
ΔS = -λ(t) · S
ΔE =  λ(t) · S  −  σ · E
ΔI =  σ · E     −  γ(t) · I  −  μ(t) · I
ΔR =  γ(t) · I
ΔD =  μ(t) · I
```

Donde la **fuerza de infección** `λ(t)` combina el contagio local con el contagio importado:

```
λ(t) = β_local(t) · (I / N)  +  Σ_conexiones [ flujo_infectados_entrantes / N ]
```

### Parámetros Dinámicos (no constantes)

La clave del realismo es que **β, γ y μ no son fijos**; se recalculan cada tick:

```java
// β local: sube con densidad, baja con inmunidad y vacunación
double calcularBetaLocal(Localidad loc) {
    double factorDensidad  = 1.0 + Math.log1p(loc.getDensidadPorKm2()) * PESO_DENSIDAD;
    double factorInmunidad = 1.0 - loc.getInmunidadEfectiva();   // vacunación + recuperados
    return BETA_BASE * factorDensidad * factorInmunidad;
}

// γ (recuperación): se degrada cuando el sistema sanitario está saturado
double calcularGamma(Localidad loc) {
    double saturacion = loc.getInfectados() / loc.getCapacidadHospitalaria();
    if (saturacion <= 1.0) return GAMMA_BASE;
    return GAMMA_BASE / (1.0 + PENALIZACION_SATURACION * (saturacion - 1.0));
}

// μ (mortalidad): sube con la edad mediana y con la saturación hospitalaria
double calcularMortalidad(Localidad loc) {
    double factorEdad       = 1.0 + PESO_EDAD * (loc.getEdadMediana() - EDAD_REFERENCIA);
    double factorSaturacion = loc.isSistemaSanitarioSaturado() ? MULT_MORTALIDAD_SATURACION : 1.0;
    return MORTALIDAD_BASE * factorEdad * factorSaturacion;
}
```

### Propagación a través de Conexiones

El contagio entre localidades se modela como un **flujo de infectados** proporcional al tráfico y al peso de la conexión:

```java
void propagarPorConexion(Conexion conexion) {
    Localidad origen  = conexion.getOrigen();
    Localidad destino = conexion.getDestino();

    // Proporción de infectados entre los viajeros del día
    double tasaInfeccionOrigen  = origen.getInfectados() / origen.getPoblacion();
    double viajerosInfectados   = conexion.getViajerosDiarios()
                                * tasaInfeccionOrigen
                                * conexion.getTipo().getMultiplicadorBase();

    // Probabilidad de que un infectado contagie en destino
    double fuerzaImportada = viajerosInfectados
                           * (1.0 - destino.getInmunidadEfectiva())
                           / destino.getPoblacion();

    destino.agregarFuerzaImportada(fuerzaImportada);   // Suma a λ(t) del destino
}
```

### Ciclo Principal

```java
public void ejecutarSimulacion(Grafo grafo, int ticks) {
    inicializarVacunacion(grafo);           // Aplica vacunación inicial: S → R

    for (int t = 0; t < ticks; t++) {
        // 1. Propagar a través de conexiones activas
        for (Conexion c : grafo.getConexionesActivas()) {
            propagarPorConexion(c);
            propagarPorConexion(c.invertida()); // Bidireccional
        }
        // 2. Evolucionar SEIR en cada localidad
        for (Localidad loc : grafo.getLocalidades()) {
            actualizarSEIR(loc);
        }
        // 3. Registrar snapshot del estado global
        historial.registrar(t, grafo);
    }
}
```

---

## ✂️ Módulo 2 — Motor de Contención Óptima

### Definición del Problema

Dado un grafo `G = (V, A)` con estado epidémico activo y un presupuesto `P`:

> Encontrar el subconjunto `C ⊆ A` de conexiones a cortar tal que se **minimice la propagación futura** y `Σ coste(a) ≤ P`.

Este es un problema de optimización combinatoria (**variante del Knapsack 0-1**).

### Medida de Impacto de una Conexión

Para decidir qué cortar, cada conexión recibe una puntuación de **impacto epidémico** que combina múltiples factores:

```java
double calcularImpactoEpidemico(Conexion conexion) {
    Localidad origen  = conexion.getOrigen();
    Localidad destino = conexion.getDestino();

    // Factor 1: flujo real de infectados por esta conexión ahora
    double flujoActual = conexion.getViajerosDiarios()
                       * (origen.getInfectados() / origen.getPoblacion())
                       * conexion.getTipo().getMultiplicadorBase();

    // Factor 2: vulnerabilidad del destino (S disponible y sin capacidad sanitaria)
    double vulnerabilidadDestino = (destino.getSusceptibles() / destino.getPoblacion())
                                 * (1.0 - destino.getInmunidadEfectiva())
                                 * calcularRiesgoSaturacion(destino);

    // Factor 3: potencial explosivo del destino (densidad amplificará el contagio)
    double potencialAmplificacion = 1.0 + Math.log1p(destino.getDensidadPorKm2()) * PESO_DENSIDAD;

    return flujoActual * vulnerabilidadDestino * potencialAmplificacion;
}
```

### Estrategias de Optimización (de menor a mayor complejidad)

#### Estrategia A — `Voraz por Ratio` *(recomendada como base)*

Ordena por `impacto / coste` y corta en orden descendente hasta agotar el presupuesto.  
Complejidad: **O(A log A)**. Buena aproximación, muy rápida.

```java
List<Conexion> optimizarVoraz(Grafo grafo, double presupuesto) {
    List<Conexion> cortes    = new ArrayList<>();
    double[] restante        = { presupuesto };

    grafo.getConexionesActivas().stream()
        .sorted(Comparator.comparingDouble(c ->
            -(calcularImpactoEpidemico(c) / c.getCosteCorte())))
        .forEach(c -> {
            if (restante[0] >= c.getCosteCorte()) {
                cortes.add(c);
                restante[0] -= c.getCosteCorte();
            }
        });
    return cortes;
}
```

#### Estrategia B — `Mochila 0-1 con PD` *(óptimo para grafos medianos)*

Garantiza la solución exacta cuando el número de aristas es manejable (< ~200).

```java
List<Conexion> optimizarMochila(List<Conexion> candidatas, int presupuesto) {
    int n = candidatas.size();
    // pd[i][p] = máximo impacto usando las primeras i candidatas con presupuesto p
    double[][] pd = new double[n + 1][presupuesto + 1];

    for (int i = 1; i <= n; i++) {
        Conexion c   = candidatas.get(i - 1);
        int coste    = (int) c.getCosteCorte();
        double impacto = calcularImpactoEpidemico(c);
        for (int p = 0; p <= presupuesto; p++) {
            pd[i][p] = pd[i-1][p];
            if (p >= coste)
                pd[i][p] = Math.max(pd[i][p], pd[i-1][p - coste] + impacto);
        }
    }
    return reconstruirSolucion(pd, candidatas, presupuesto);
}
```

#### Estrategia C — `Centralidad de Flujo Epidémico` *(estratégica para grafos grandes)*

Calcula la centralidad de cada arista en términos de **cuánto flujo de enfermedad pasa por ella** usando Dijkstra ponderado, sin librerías externas:

```java
Map<Conexion, Double> calcularCentralidadEpidemica(Grafo grafo) {
    Map<Conexion, Double> centralidad = new HashMap<>();

    // Para cada localidad infectada, calcular el alcance de propagación
    // y acumular peso sobre los caminos más probables de contagio
    for (Localidad foco : grafo.getLocalidadesInfectadas()) {
        Map<Localidad, Double> alcance = dijkstraPorTransmision(grafo, foco);
        for (Map.Entry<Localidad, Double> entrada : alcance.entrySet()) {
            acumularEnCamino(centralidad, foco, entrada.getKey(), entrada.getValue());
        }
    }
    return centralidad;
}
```

#### Flujo de Decisión del Optimizador

```
¿Número de conexiones candidatas?
       ├─ < 150  →  Mochila PD       (solución exacta)
       ├─ < 500  →  Voraz por Ratio  (aproximación rápida)
       └─ > 500  →  Centralidad de Flujo + Voraz sobre top-K candidatas
```

---

## 🏗️ Estructura Interna del Grafo

Implementado con **Java estándar** usando listas de adyacencia:

```java
public class Grafo {
    private final Map<String, Localidad>         localidades;      // id → Localidad
    private final Map<String, List<Conexion>>    adyacencia;       // id → conexiones salientes
    private final List<Conexion>                 todasConexiones;

    // Iteración eficiente sobre conexiones activas
    public List<Conexion> getConexionesActivas() {
        return todasConexiones.stream()
            .filter(Conexion::isActiva)
            .collect(Collectors.toList());
    }

    // Vecinos de una localidad (para propagación)
    public List<Conexion> getConexionesDe(String idLocalidad) {
        return adyacencia.getOrDefault(idLocalidad, Collections.emptyList());
    }
}
```

---

## 🔄 Flujo de Ejecución Completo

```
[1] Carga del grafo (JSON / CSV con Java estándar)
         ↓
[2] Inicialización: aplicar vacunación, definir focos iniciales de infección
         ↓
[3] Simulación libre N ticks → establece estado base de propagación
         ↓
[4] Instantánea: registrar infectados proyectados sin intervención
         ↓
[5] Optimizador: evaluar impacto epidémico de cada conexión activa
         ↓
[6] Seleccionar cortes óptimos dentro del presupuesto P
         ↓
[7] Aplicar cortes (Conexion.activa = false)
         ↓
[8] Simulación con cortes aplicados N ticks adicionales
         ↓
[9] Informe comparativo: Δinfectados, Δfallecidos, presupuesto usado, eficiencia
```

---

## 📊 Métricas del Sistema

| Métrica | Cálculo |
|--------|---------|
| `reproductividad_efectiva(t)` | `β_efectivo(t) · S(t)/N · (1/γ)` — indica si la epidemia crece o decrece |
| `pico_infectados` | Máximo de `I(t)` en toda la simulación |
| `dias_saturacion_sanitaria` | Días que el sistema sanitario de cualquier localidad estuvo saturado |
| `eficiencia_contencion` | `(ΔI_evitados) / presupuesto_usado` |
| `impacto_social_cortes` | Suma de `costeSocial` de todas las conexiones cortadas |

---

## 📁 Formato de Entrada (JSON, parseado con `javax.json` o manualmente)

```json
{
  "localidades": [
    {
      "id": "LOC_001",
      "nombre": "Valle Norte",
      "poblacion": 85000,
      "densidad_km2": 320,
      "edad_mediana": 41,
      "proporcion_mayores": 0.19,
      "camas_por_1000": 3.2,
      "inmunidad_base": 0.05,
      "tasa_vacunacion": 0.60
    }
  ],
  "conexiones": [
    {
      "origen": "LOC_001",
      "destino": "LOC_002",
      "viajeros_diarios": 4200,
      "tipo": "TERRESTRE",
      "coste_corte": 12000,
      "coste_social": 8500
    }
  ]
}
```

---

## 📦 Dependencias

| Librería | Motivo | Alternativa nativa |
|---------|--------|-------------------|
| *(ninguna obligatoria)* | El grafo, SEIR y optimizador se implementan en Java puro | — |
| `org.json` *(opcional)* | Parseo cómodo de JSON de entrada | `java.util.Properties` o parser manual |
| `JFreeChart` *(opcional)* | Gráficas de evolución temporal | Exportar CSV y visualizar externamente |

> ✅ El núcleo funcional completo — simulación + grafo + optimización — **no requiere ninguna librería externa**.

*EpiGrafo · Java 17+ · Sin dependencias externas obligatorias · Modelado realista sobre grafos*
