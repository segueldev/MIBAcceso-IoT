package cl.seguel.mib

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CredencialesTest {
    private lateinit var contexto: Context
    @Before fun preparar() {
        contexto = RuntimeEnvironment.getApplication()
        contexto.deleteDatabase("credencial_mib.db")
        contexto.getSharedPreferences("credencial_mib", 0).edit().clear().commit()
    }

    @Test fun noHayClaveInicialYElBloqueoPersisteAlRecrearElGestor() {
        val credenciales = Credenciales(contexto)
        assertFalse(credenciales.existe)
        credenciales.crear("agenteB", "Contrasena-de-prueba-42".toCharArray())
        repeat(3) { assertFalse(credenciales.verificar("agenteB", "incorrecta".toCharArray())) }
        val reabiertas = Credenciales(contexto)
        assertTrue(reabiertas.segundosBloqueo() > 0)
        assertFalse(reabiertas.verificar("agenteB", "Contrasena-de-prueba-42".toCharArray()))
        val guardado = AlmacenSqlite(contexto, "credencial_mib").getString("verificador", "")!!
        assertFalse(guardado.contains("Contrasena-de-prueba-42"))
    }

    @Test fun validaUsuarioYContrasenaYNoPermiteReemplazarLaCuenta() {
        val credenciales = Credenciales(contexto)
        assertThrows(IllegalArgumentException::class.java) { credenciales.crear("agente", "1234".toCharArray()) }
        credenciales.crear("agenteB", "Contrasena-de-prueba-42".toCharArray())
        assertFalse(credenciales.verificar("otroAgente", "Contrasena-de-prueba-42".toCharArray()))
        assertTrue(credenciales.verificar("agenteB", "Contrasena-de-prueba-42".toCharArray()))
        assertThrows(IllegalArgumentException::class.java) { credenciales.crear("otro", "Otra-contrasena-42".toCharArray()) }
    }
}
