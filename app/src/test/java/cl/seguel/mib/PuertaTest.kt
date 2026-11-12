package cl.seguel.mib

import org.junit.Assert.*
import org.junit.Test

class PuertaTest {
    @Test fun exigePresenciaYMantenimientoDesactivado() {
        val puerta = Puerta()
        assertTrue(puerta.abrir("1", 1, 4, 1000).startsWith("RECHAZADO"))
        puerta.presencia = true
        puerta.mantenimiento = true
        assertTrue(puerta.abrir("2", 1, 4, 1000).startsWith("RECHAZADO"))
        puerta.mantenimiento = false
        assertTrue(puerta.abrir("3", 1, 4, 1000).startsWith("ACEPTADO"))
    }

    @Test fun cierraEnCuatroSegundosYUnaOrdenDuplicadaNoReabre() {
        val puerta = Puerta().apply { presencia = true }
        val respuesta = puerta.abrir("misma-orden", 2, 4, 1000)
        assertTrue(puerta.abierta(4999))
        assertFalse(puerta.abierta(5000))
        assertEquals(respuesta, puerta.abrir("misma-orden", 2, 4, 8000))
        assertFalse(puerta.abierta(8000))
        assertEquals(5000L, puerta.abiertaHasta)
    }

    @Test fun rechazaParametrosQuePodrianDejarLaPuertaAbierta() {
        val puerta = Puerta().apply { presencia = true }
        val fueraDeRango = listOf(
            0 to 4,
            (AjustesAgencia.sectores.size + 1) to 4,
            1 to (AjustesAgencia.DURACION_MINIMA - 1),
            1 to (AjustesAgencia.DURACION_MAXIMA + 1),
            1 to Int.MAX_VALUE
        )
        for ((i, datos) in fueraDeRango.withIndex()) {
            assertTrue(puerta.abrir("orden-$i", datos.first, datos.second, 1000).startsWith("RECHAZADO"))
        }
        assertFalse(puerta.abierta(1001))
    }
}
