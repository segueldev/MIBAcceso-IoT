package cl.seguel.mib

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class Credenciales(contexto: Context) {
    private val preferencias = AlmacenSqlite(contexto, "credencial_mib")
    val existe get() = preferencias.contains("verificador")
    val usuario get() = preferencias.getString("usuario", "")!!
    fun segundosBloqueo(): Long = ((preferencias.getLong("bloqueo", 0) - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0)

    fun crear(usuario: String, contrasena: CharArray) {
        require(!existe) { "Ya existe una credencial en este dispositivo." }
        require(usuario.matches(Regex("[A-Za-z0-9._-]{3,24}"))) { "Usuario: 3 a 24 letras, números, puntos o guiones." }
        require(contrasena.size in 12..128) { "Usa una contraseña de 12 a 128 caracteres." }
        val sal = Seguridad.aleatorio(16)
        val hash = Seguridad.derivar(contrasena, sal)
        check(preferencias.edit().putString("usuario", usuario).putString("sal", codificar(sal))
            .putString("verificador", codificar(hash)).commit()) { "No se pudo guardar la credencial." }
    }

    @Synchronized fun verificar(nombre: String, contrasena: CharArray): Boolean {
        if (!existe || segundosBloqueo() > 0) return false
        val calculado = Seguridad.derivar(contrasena, decodificar(preferencias.getString("sal", "")!!))
        val valida = Seguridad.iguales(calculado, decodificar(preferencias.getString("verificador", "")!!)) && nombre == usuario
        val editor = preferencias.edit()
        if (valida) editor.putInt("fallos", 0).putLong("bloqueo", 0)
        else {
            val fallos = preferencias.getInt("fallos", 0) + 1
            if (fallos >= 3) editor.putInt("fallos", 0).putLong("bloqueo", System.currentTimeMillis() + 60_000)
            else editor.putInt("fallos", fallos)
        }
        editor.commit()
        return valida
    }

    // Guardamos el codigo de mision cifrado con Android Keystore para no dejarlo en texto plano
    fun codigoBase(): String {
        val guardado = preferencias.getString("mision", null)
        if (guardado == null) return renovarCodigo()
        val partes = guardado.split(":")
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.DECRYPT_MODE, claveLocal(), GCMParameterSpec(128, decodificar(partes[0])))
        return String(cifrador.doFinal(decodificar(partes[1])), Charsets.UTF_8)
    }

    fun renovarCodigo(): String {
        val codigo = Seguridad.codigoNuevo()
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.ENCRYPT_MODE, claveLocal())
        val contenido = codificar(cifrador.iv) + ":" + codificar(cifrador.doFinal(codigo.toByteArray(Charsets.UTF_8)))
        check(preferencias.edit().putString("mision", contenido).commit())
        return codigo
    }

    private fun claveLocal(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey("mib_mision", null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(KeyGenParameterSpec.Builder("mib_mision", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generador.generateKey()
    }

    private fun codificar(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decodificar(texto: String) = Base64.decode(texto, Base64.NO_WRAP)
}
