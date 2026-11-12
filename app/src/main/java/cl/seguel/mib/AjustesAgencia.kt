package cl.seguel.mib

// Cambios habituales para practicar: tiempos, destinos y mensajes de la puerta.
// La pantalla y la base usan estos mismos valores.
object AjustesAgencia {
    // Segundos. Mantener 0 < MINIMA < MAXIMA e INICIAL dentro de ese intervalo (incluidos los extremos).
    const val DURACION_MINIMA = 1
    const val DURACION_INICIAL = 4
    const val DURACION_MAXIMA = 10

    // Cada posición corresponde a un número de sector: 1, 2, 3...
    // Son destinos de una misma puerta simulada, no cerraduras independientes.
    val sectores = listOf(
        "Archivo alienígena",
        "Laboratorio",
        "Sala del neuralizador"
    )

    // Cambiar el texto conserva las reglas que aceptan o rechazan el acceso.
    const val ACCESO_AUTORIZADO = "Acceso autorizado. Bienvenido a MIB."
    const val SIN_PRESENCIA = "No hay nadie frente a la puerta. ¿Agente invisible?"
    const val EN_MANTENIMIENTO = "Sector en mantenimiento. Ni los agentes pasan."
}
