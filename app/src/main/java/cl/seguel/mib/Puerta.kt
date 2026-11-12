package cl.seguel.mib

// Logica de control de la puerta (ejecutada en la Base MIB)
class Puerta {
    var abiertaHasta = 0L
        private set
    var presencia = false
    var mantenimiento = false
    var sector = 1
        private set
    private val procesadas = LinkedHashMap<String, String>()

    fun abierta(ahora: Long) = abiertaHasta > ahora
    fun abrir(id: String, sectorSolicitado: Int, segundos: Int, ahora: Long): String {
        // Evitamos procesar dos veces la misma solicitud si llega repetida
        procesadas[id]?.let { return it }
        val resultado = when {
            sectorSolicitado !in 1..AjustesAgencia.sectores.size ||
                segundos !in AjustesAgencia.DURACION_MINIMA..AjustesAgencia.DURACION_MAXIMA ->
                "RECHAZADO|Parámetros fuera de rango."
            mantenimiento -> "RECHAZADO|${AjustesAgencia.EN_MANTENIMIENTO}"
            !presencia -> "RECHAZADO|${AjustesAgencia.SIN_PRESENCIA}"
            abierta(ahora) -> "RECHAZADO|La puerta ya está abierta. Pase de a uno."
            else -> {
                sector = sectorSolicitado
                // Guardamos la marca de tiempo de cierre sin congelar la app con un Thread.sleep
                abiertaHasta = ahora + segundos * 1000L
                "ACEPTADO|${AjustesAgencia.ACCESO_AUTORIZADO}"
            }
        }
        procesadas[id] = resultado
        if (procesadas.size > 256) procesadas.remove(procesadas.keys.first())
        return resultado
    }

    fun cerrar() {
        abiertaHasta = 0
    }

    // Formato de telemetria: estado|presencia|mantenimiento|sector|segundosRestantes
    fun estado(ahora: Long): String = listOf(
        if (abierta(ahora)) "ABIERTA" else "CERRADA",
        if (presencia) "1" else "0",
        if (mantenimiento) "1" else "0",
        sector.toString(),
        ((abiertaHasta - ahora).coerceAtLeast(0) / 1000).toString()
    ).joinToString("|")
}
