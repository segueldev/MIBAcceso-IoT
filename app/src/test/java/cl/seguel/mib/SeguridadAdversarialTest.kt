package cl.seguel.mib

import java.io.*
import java.net.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class SeguridadAdversarialTest {
    @Test fun codigoIncorrectoNoCompletaElSaludo() {
        val servidor = ServerSocket(0)
        val ejecutor = Executors.newSingleThreadExecutor()
        try {
            val resultado = ejecutor.submit<Boolean> {
                try { CanalSeguro.abrir(servidor.accept(), Seguridad.codigoNuevo(), true).cerrar(); false }
                catch (_: IOException) { true }
            }
            assertThrows(IOException::class.java) {
                CanalSeguro.abrir(Socket("127.0.0.1", servidor.localPort), Seguridad.codigoNuevo(), false)
            }
            assertTrue(resultado.get(6, TimeUnit.SECONDS))
        } finally { servidor.close(); ejecutor.shutdownNow() }
    }
    @Test fun tramaTruncadaONegativaSeRechaza() {
        val clave = Seguridad.aleatorio(32)
        for (tamano in listOf(-1, 27, 8193)) {
            val bytes = ByteArrayOutputStream()
            DataOutputStream(bytes).writeInt(tamano)
            assertThrows(IllegalArgumentException::class.java) {
                Cifrado.leer(DataInputStream(ByteArrayInputStream(bytes.toByteArray())), clave)
            }
        }
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).apply { writeInt(200); write(ByteArray(3)) }
        assertThrows(EOFException::class.java) {
            Cifrado.leer(DataInputStream(ByteArrayInputStream(bytes.toByteArray())), clave)
        }
    }
    @Test fun sesionAjenaSeRechazaInclusoConClaveCorrecta() {
        val servidor = ServerSocket(0)
        val ejecutor = Executors.newSingleThreadExecutor()
        val codigo = Seguridad.codigoNuevo()
        try {
            val resultado = ejecutor.submit<Boolean> {
                val base = CanalSeguro.abrir(servidor.accept(), codigo, true)
                try { base.recibir(); false } catch (_: IllegalArgumentException) { true }
                finally { base.cerrar() }
            }
            val agente = CanalSeguro.abrir(Socket("127.0.0.1", servidor.localPort), codigo, false)
            try {
                Cifrado.escribir(DataOutputStream(agente.socket.getOutputStream()),
                    Mensaje(Tipo.ABRIR, sesion = "sesion-ajena", secuencia = 3, datos = "1|4"), Seguridad.leerCodigo(codigo))
                assertTrue(resultado.get(5, TimeUnit.SECONDS))
            } finally { agente.cerrar() }
        } finally { servidor.close(); ejecutor.shutdownNow() }
    }
}
