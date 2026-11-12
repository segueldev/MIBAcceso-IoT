package cl.seguel.mib

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlmacenSqliteTest {
    @Test fun migraDatosAnterioresYLosConservaAlReabrir() {
        val c = RuntimeEnvironment.getApplication()
        c.getSharedPreferences("migracion", 0).edit().putString("eventos", "registro").putInt("fallos", 2).commit()
        val almacen = AlmacenSqlite(c, "migracion")
        assertEquals("registro", almacen.getString("eventos", null))
        assertTrue(c.getSharedPreferences("migracion", 0).all.isEmpty())
        assertEquals(2, almacen.getInt("fallos", 0))
        almacen.edit().putLong("bloqueo", 12345).putString("eventos", "nuevo registro").commit()
        val reabierto = AlmacenSqlite(c, "migracion")
        assertEquals(12345L, reabierto.getLong("bloqueo", 0))
        assertEquals("nuevo registro", reabierto.getString("eventos", null))
        assertFalse(reabierto.contains("inexistente"))
    }
}
