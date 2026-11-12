package cl.seguel.mib

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatosSegurosTest {
    @Test fun consultasParametrizadasNoInterpretanSqlEnLasClaves() {
        val almacen = AlmacenSqlite(RuntimeEnvironment.getApplication(), "inyeccion")
        val clave = "x' OR 1=1 --"
        almacen.edit().putString("normal", "reservado").putString(clave, "literal").commit()
        assertEquals("literal", almacen.getString(clave, null))
        assertEquals("reservado", almacen.getString("normal", null))
        assertNull(almacen.getString("' OR 1=1 --", null))
    }
    @Test fun exportacionNeutralizaFormulasYEscapaComillas() {
        val modelo = ModeloAgencia(RuntimeEnvironment.getApplication())
        modelo.eventos.clear()
        modelo.eventos.add(Evento("2026-11-12", "=1+1", "+SUM(1,2)"))
        modelo.eventos.add(Evento("2026-11-12", "texto con \"comillas\"", "@SUM(1,2)"))
        val csv = modelo.exportarCsv()
        assertTrue(csv.contains("\"'=1+1\""))
        assertTrue(csv.contains("\"'+SUM(1,2)\""))
        assertTrue(csv.contains("\"'@SUM(1,2)\""))
        assertTrue(csv.contains("texto con \"\"comillas\"\""))
    }
}
