package cl.seguel.mib

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class Tipo { HOLA, BIENVENIDO, LISTO, CONSULTAR, ABRIR, RESULTADO, ESTADO, PING, PONG }

data class Mensaje(
    val tipo: Tipo, val id: String = UUID.randomUUID().toString(),
    val sesion: String = "", val secuencia: Long = 0, val datos: String = ""
)

// Funciones de seguridad y hashes para la comunicacion y login
object Seguridad {
    private val random = SecureRandom()
    fun aleatorio(tamano: Int) = ByteArray(tamano).also(random::nextBytes)
    fun codigoNuevo(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio(32))
    fun leerCodigo(codigo: String): ByteArray {
        require(codigo.matches(Regex("[A-Za-z0-9_-]{43}"))) { "El código de misión debe tener 43 caracteres." }
        return Base64.getUrlDecoder().decode(codigo).also { require(it.size == 32) }
    }
    fun derivar(contrasena: CharArray, sal: ByteArray): ByteArray {
        // Derivamos la contrasena con PBKDF2 para guardarla en SQLite sin almacenar el texto original
        val spec = PBEKeySpec(contrasena, sal, 210_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    fun iguales(a: ByteArray, b: ByteArray) = MessageDigest.isEqual(a, b)
}

object Cifrado {
    const val MAX_TRAMA = 8192
    private val contexto = "MIB-ACCESO-v1".toByteArray(Charsets.UTF_8)

    fun cifrar(mensaje: Mensaje, clave: ByteArray): ByteArray {
        val contenido = ByteArrayOutputStream()
        DataOutputStream(contenido).use {
            it.writeInt(1)
            it.writeUTF(mensaje.tipo.name)
            it.writeUTF(mensaje.id)
            it.writeUTF(mensaje.sesion)
            it.writeLong(mensaje.secuencia)
            it.writeUTF(mensaje.datos)
        }
        require(contenido.size() <= 4096) { "Mensaje demasiado largo." }
        val iv = Seguridad.aleatorio(12)
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.ENCRYPT_MODE, SecretKeySpec(clave, "AES"), GCMParameterSpec(128, iv))
        cifrador.updateAAD(contexto)
        return iv + cifrador.doFinal(contenido.toByteArray())
    }

    fun descifrar(trama: ByteArray, clave: ByteArray): Mensaje {
        require(trama.size in 28..MAX_TRAMA) { "Tamaño de trama inválido." }
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.DECRYPT_MODE, SecretKeySpec(clave, "AES"), GCMParameterSpec(128, trama.copyOfRange(0, 12)))
        cifrador.updateAAD(contexto)
        val entrada = DataInputStream(ByteArrayInputStream(cifrador.doFinal(trama.copyOfRange(12, trama.size))))
        require(entrada.readInt() == 1) { "Versión de protocolo desconocida." }
        val mensaje = Mensaje(Tipo.valueOf(entrada.readUTF()), entrada.readUTF(), entrada.readUTF(), entrada.readLong(), entrada.readUTF())
        require(entrada.available() == 0 && mensaje.id.length <= 64 && mensaje.sesion.length <= 64)
        return mensaje
    }

    fun escribir(salida: DataOutputStream, mensaje: Mensaje, clave: ByteArray) {
        val trama = cifrar(mensaje, clave)
        salida.writeInt(trama.size)
        salida.write(trama)
        salida.flush()
    }

    fun leer(entrada: DataInputStream, clave: ByteArray): Mensaje {
        val tamano = entrada.readInt()
        require(tamano in 28..MAX_TRAMA) { "Tamaño de trama inválido." }
        val trama = ByteArray(tamano)
        entrada.readFully(trama) // TCP puede entregar un mensaje en varios fragmentos.
        return descifrar(trama, clave)
    }
}

class CanalSeguro private constructor(
    val socket: Socket, private val clave: ByteArray, private val entrada: DataInputStream,
    private val salida: DataOutputStream, val sesion: String,
    private var enviada: Long, private var recibida: Long
) {
    @Synchronized fun enviar(tipo: Tipo, datos: String = "", id: String = UUID.randomUUID().toString()) {
        Cifrado.escribir(salida, Mensaje(tipo, id, sesion, ++enviada, datos), clave)
    }

    fun recibir(): Mensaje {
        val mensaje = Cifrado.leer(entrada, clave)
        // Validamos que el mensaje corresponda a la sesion activa y no sea un paquete repetido
        require(mensaje.sesion == sesion && mensaje.secuencia == recibida + 1) { "Mensaje repetido o fuera de sesión." }
        require(mensaje.tipo !in listOf(Tipo.HOLA, Tipo.BIENVENIDO, Tipo.LISTO))
        recibida = mensaje.secuencia
        return mensaje
    }

    fun cerrar() { runCatching { socket.close() }; clave.fill(0) }

    companion object {
        fun abrir(socket: Socket, codigo: String, esBase: Boolean): CanalSeguro {
            val clave = Seguridad.leerCodigo(codigo)
            socket.soTimeout = 5000
            socket.tcpNoDelay = true
            val entrada = DataInputStream(socket.getInputStream())
            val salida = DataOutputStream(socket.getOutputStream())
            try {
                val sesion: String
                if (esBase) {
                    val hola = Cifrado.leer(entrada, clave)
                    require(hola.tipo == Tipo.HOLA && hola.secuencia == 1L && hola.sesion.isEmpty())
                    require(hola.datos.matches(Regex("[A-Za-z0-9_-]{43}")))
                    sesion = UUID.randomUUID().toString()
                    Cifrado.escribir(salida, Mensaje(Tipo.BIENVENIDO, sesion = sesion, secuencia = 1, datos = hola.datos), clave)
                    val listo = Cifrado.leer(entrada, clave)
                    require(listo.tipo == Tipo.LISTO && listo.sesion == sesion && listo.secuencia == 2L && listo.datos == hola.datos)
                } else {
                    val reto = Seguridad.codigoNuevo()
                    Cifrado.escribir(salida, Mensaje(Tipo.HOLA, secuencia = 1, datos = reto), clave)
                    val respuesta = Cifrado.leer(entrada, clave)
                    require(respuesta.tipo == Tipo.BIENVENIDO && respuesta.secuencia == 1L && respuesta.datos == reto)
                    UUID.fromString(respuesta.sesion)
                    sesion = respuesta.sesion
                    Cifrado.escribir(salida, Mensaje(Tipo.LISTO, sesion = sesion, secuencia = 2, datos = reto), clave)
                }
                socket.soTimeout = 15_000
                return CanalSeguro(socket, clave, entrada, salida, sesion, if (esBase) 1 else 2, if (esBase) 2 else 1)
            } catch (e: Exception) {
                clave.fill(0)
                runCatching { socket.close() }
                throw IOException("No se pudo verificar el código de misión o la conexión.", e)
            }
        }
    }
}
