package cl.seguel.mib

import android.widget.TextView
import android.os.Looper
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.slider.Slider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class InicioTest {
    @Test fun iniciaConFormularioDeCredencialSinAccesoAlPanel() {
        val controlador = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val actividad = controlador.get()
            assertNotNull(actividad.findViewById<TextView>(R.id.usuario))
            assertNotNull(actividad.findViewById<TextView>(R.id.contrasena))
            assertNotNull(actividad.findViewById<TextView>(R.id.ingresar))
            assertNull(actividad.findViewById<TextView>(R.id.nav_activador))
        } finally { controlador.pause().stop().destroy() }
    }

    @Test fun ajustesDeDuracionLleganAlControlAlModeloYALaPuerta() {
        val controlador = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val actividad = controlador.get()
            actividad.getSharedPreferences("credencial_mib", 0).edit().clear().commit()
            val modelo = ViewModelProvider(actividad)[ModeloAgencia::class.java]
            modelo.autenticar("agentePrueba", "Una-contrasena-2026".toCharArray(), true)
            val limite = System.nanoTime() + 10_000_000_000L
            do {
                Thread.sleep(20)
                shadowOf(Looper.getMainLooper()).idle()
            } while (modelo.ocupado && System.nanoTime() < limite)
            assertTrue("El login de práctica debe terminar", modelo.sesion)

            actividad.findViewById<View>(R.id.nav_activador).performClick()
            val control = actividad.findViewById<Slider>(R.id.duracion)
            assertEquals(AjustesAgencia.DURACION_INICIAL.toFloat(), control.value, 0f)
            assertEquals(AjustesAgencia.DURACION_MINIMA.toFloat(), control.valueFrom, 0f)
            assertEquals(AjustesAgencia.DURACION_MAXIMA.toFloat(), control.valueTo, 0f)

            control.value = control.valueTo
            assertEquals(AjustesAgencia.DURACION_MAXIMA, modelo.duracion)
            val puertaBase = Puerta().apply { presencia = true }
            assertTrue(puertaBase.abrir("ajuste", 1, modelo.duracion, 1000).startsWith("ACEPTADO"))
            val cierre = 1000L + modelo.duracion * 1000L
            assertTrue(puertaBase.abierta(cierre - 1))
            assertFalse(puertaBase.abierta(cierre))
            assertTrue(puertaBase.abrir("exceso", 1, modelo.duracion + 1, cierre).startsWith("RECHAZADO"))
        } finally { controlador.pause().stop().destroy() }
    }
}
