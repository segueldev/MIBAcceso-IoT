package cl.seguel.mib

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CanalSeguroTest {
    @Test fun cifradoNoExponeDatosYDetectaAlteracion() {
        val clave = Seguridad.aleatorio(32)
        val mensaje = Mensaje(Tipo.ABRIR, datos = "SECTOR-SECRETO")
        val cifrado = Cifrado.cifrar(mensaje, clave)
        assertFalse(String(cifrado).contains("SECTOR-SECRETO"))
        assertEquals(mensaje, Cifrado.descifrar(cifrado, clave))
        cifrado[cifrado.lastIndex] = (cifrado.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { Cifrado.descifrar(cifrado, clave) }
    }

    @Test fun rechazaClaveEquivocadaYTramasGigantes() {
        val trama = Cifrado.cifrar(Mensaje(Tipo.CONSULTAR), Seguridad.aleatorio(32))
        assertThrows(Exception::class.java) { Cifrado.descifrar(trama, Seguridad.aleatorio(32)) }
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).writeInt(Int.MAX_VALUE)
        assertThrows(IllegalArgumentException::class.java) { Cifrado.leer(DataInputStream(ByteArrayInputStream(bytes.toByteArray())), Seguridad.aleatorio(32)) }
        assertThrows(IllegalArgumentException::class.java) { Seguridad.leerCodigo("1234") }
    }

    @Test fun cadaCifradoTieneUnIvDiferente() {
        val clave = Seguridad.aleatorio(32)
        val mensaje = Mensaje(Tipo.ABRIR)
        val uno = Cifrado.cifrar(mensaje, clave)
        val dos = Cifrado.cifrar(mensaje, clave)
        assertFalse(uno.contentEquals(dos))
        assertEquals(mensaje, Cifrado.descifrar(dos, clave))
    }

    @Test fun dosExtremosRealesIntercambianOrdenYConfirmacion() {
        val servidor = ServerSocket(0)
        val ejecutor = Executors.newSingleThreadExecutor()
        val codigo = Seguridad.codigoNuevo()
        try {
            val base = ejecutor.submit<String> {
                val canal = CanalSeguro.abrir(servidor.accept(), codigo, true)
                try {
                    val orden = canal.recibir()
                    assertEquals(Tipo.ABRIR, orden.tipo)
                    assertEquals("1|4", orden.datos)
                    canal.enviar(Tipo.RESULTADO, "ACEPTADO|Acceso autorizado", orden.id)
                    canal.sesion
                } finally { canal.cerrar() }
            }
            val agente = CanalSeguro.abrir(Socket("127.0.0.1", servidor.localPort), codigo, false)
            try {
                agente.enviar(Tipo.ABRIR, "1|4", "orden-prueba")
                val respuesta = agente.recibir()
                assertEquals("orden-prueba", respuesta.id)
                assertEquals(Tipo.RESULTADO, respuesta.tipo)
                assertEquals(agente.sesion, base.get(5, TimeUnit.SECONDS))
            } finally { agente.cerrar() }
        } finally { servidor.close(); ejecutor.shutdownNow() }
    }

    @Test fun rechazaSecuenciaRepetidaAunqueElCifradoSeaValido() {
        val servidor = ServerSocket(0)
        val ejecutor = Executors.newSingleThreadExecutor()
        val codigo = Seguridad.codigoNuevo()
        try {
            val base = ejecutor.submit<Boolean> {
                val canal = CanalSeguro.abrir(servidor.accept(), codigo, true)
                try {
                    canal.recibir()
                    try { canal.recibir(); false } catch (_: IllegalArgumentException) { true }
                } finally { canal.cerrar() }
            }
            val agente = CanalSeguro.abrir(Socket("127.0.0.1", servidor.localPort), codigo, false)
            try {
                agente.enviar(Tipo.CONSULTAR)
                // Después del saludo el primer mensaje del agente lleva secuencia 3.
                val repetida = Mensaje(Tipo.ABRIR, sesion = agente.sesion, secuencia = 3, datos = "1|4")
                Cifrado.escribir(DataOutputStream(agente.socket.getOutputStream()), repetida, Seguridad.leerCodigo(codigo))
                assertTrue(base.get(5, TimeUnit.SECONDS))
            } finally { agente.cerrar() }
        } finally { servidor.close(); ejecutor.shutdownNow() }
    }
}
