package cl.seguel.mib

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

data class Evento(val fecha: String, val detalle: String, val solicitud: String = "", val rtt: Long? = null)

class ModeloAgencia(aplicacion: Application) : AndroidViewModel(aplicacion) {
    val credenciales = Credenciales(aplicacion)
    val puerta = Puerta()
    private val handler = Handler(Looper.getMainLooper())
    private val trabajo = Executors.newSingleThreadExecutor()
    private val preferencias = AlmacenSqlite(aplicacion, "registro_mib")
    val eventos = mutableListOf<Evento>()
    var observar: (() -> Unit)? = null
    var sesion = false
        private set
    var ocupado = false
        private set
    var esBase = false
        private set
    var conectado = false
        private set
    var enlace = "Sin conexión"
        private set
    var aviso = "Identifíquese. La corbata no cuenta como credencial."
        private set
    var direccion = "10.0.2.2"
    var codigoAgente = ""
    var sectorElegido = 1
    var duracion = AjustesAgencia.DURACION_INICIAL
    var estadoRemoto: List<String>? = null
        private set
    var ultimaTelemetria = 0L
        private set
    var enviados = 0
        private set
    var confirmados = 0
        private set
    var agotados = 0
        private set
    val tiempos = mutableListOf<Long>()
    private var solicitudPendiente: String? = null
    private var inicioSolicitud = 0L
    private var ultimaActividad = SystemClock.elapsedRealtime()
    private var ultimoLatido = 0L
    private var ultimoRecibido = 0L
    private var versionSesion = 0
    private val conexion: ConexionWifi = ConexionWifi { turno, estado, mensaje ->
        // La red avisa desde otro hilo. Handler permite actualizar la interfaz en el principal.
        handler.post { if (turno == conexionGeneracion()) recibirEvento(estado, mensaje) }
    }

    init {
        runCatching {
            val lista = JSONArray(preferencias.getString("eventos", "[]"))
            for (i in 0 until lista.length()) {
                val o = lista.getJSONObject(i)
                eventos.add(Evento(o.getString("fecha"), o.getString("detalle"), o.optString("id"), if (o.has("rtt")) o.getLong("rtt") else null))
            }
        }
        handler.post(object : Runnable {
            override fun run() { actualizarReloj(); handler.postDelayed(this, 1000) }
        })
    }

    private fun conexionGeneracion(): Int = conexion.generacion
    fun actividad() { ultimaActividad = SystemClock.elapsedRealtime() }
    fun informar(texto: String) { aviso = texto; observar?.invoke() }

    fun autenticar(usuario: String, contrasena: CharArray, crear: Boolean) {
        if (ocupado || sesion) { contrasena.fill('\u0000'); return }
        ocupado = true
        observar?.invoke()
        trabajo.execute {
            var error: String? = null
            val valido = try {
                if (crear) credenciales.crear(usuario.trim(), contrasena)
                credenciales.verificar(usuario.trim(), contrasena)
            } catch (e: Exception) { error = e.message ?: "No se pudo guardar la credencial."; false }
            finally { contrasena.fill('\u0000') }
            handler.post {
                ocupado = false
                sesion = valido
                if (valido) versionSesion++
                actividad()
                aviso = if (valido) "Credencial verificada. Bienvenido a MIB." else error ?: "Buen intento, terrícola. Revisa usuario y contraseña."
                registrar(if (valido) "Inicio de sesión autorizado" else "Intento de inicio rechazado")
                observar?.invoke()
            }
        }
    }

    fun salir() {
        versionSesion++
        desconectar()
        puerta.cerrar()
        sesion = false
        codigoAgente = ""
        registrar("Sesión cerrada")
        informar("Sesión cerrada. Usted no estuvo aquí.")
    }

    fun cambiarRol(base: Boolean) {
        if (esBase == base) return
        desconectar()
        puerta.cerrar()
        esBase = base
        informar(if (base) "Este equipo representa la puerta. No hay cerradura física conectada." else "Este equipo solicita acceso a la Base MIB.")
    }

    fun conectar(host: String, codigo: String) {
        if (!sesion || ocupado) return
        if (esBase) prepararCodigo(false) { iniciarConexion(host, it) }
        else iniciarConexion(host, codigo.trim())
    }

    fun prepararCodigo(renovar: Boolean, listo: (String) -> Unit) {
        if (!sesion || ocupado) return
        ocupado = true
        val permiso = versionSesion
        informar("Consultando el código de misión en el almacenamiento seguro…")
        trabajo.execute {
            val resultado = runCatching { if (renovar) credenciales.renovarCodigo() else credenciales.codigoBase() }
            handler.post {
                ocupado = false
                if (sesion && versionSesion == permiso) {
                    resultado.onSuccess(listo).onFailure { informar("No se pudo acceder al código en Android Keystore.") }
                    observar?.invoke()
                }
            }
        }
    }

    private fun iniciarConexion(host: String, clave: String) {
        if (!sesion) return
        try {
            Seguridad.leerCodigo(clave).fill(0)
            val destino = host.trim()
            require(esBase || destino.matches(Regex("[A-Za-z0-9.:-]{1,253}"))) { "Revisa la dirección de la base." }
            direccion = destino
            if (!esBase) codigoAgente = clave
            desconectar()
            enlace = if (esBase) "Iniciando Base MIB…" else "Conectando con la base…"
            conexion.iniciar(esBase, destino, clave)
            informar("Verificando código de misión…")
        } catch (e: Exception) { informar(e.message ?: "No se pudo iniciar la conexión.") }
    }

    fun desconectar() {
        conexion.cerrar()
        conectado = false
        estadoRemoto = null
        if (solicitudPendiente != null) {
            agotados++
            registrar("Enlace perdido: resultado de apertura no confirmado", solicitudPendiente!!)
        }
        solicitudPendiente = null
        enlace = "Sin conexión"
        observar?.invoke()
    }

    fun cambiarPresencia(valor: Boolean) { puerta.presencia = valor; registrar(if (valor) "Presencia detectada" else "Presencia finalizada"); enviarEstado(); observar?.invoke() }
    fun cambiarMantenimiento(valor: Boolean) {
        puerta.mantenimiento = valor
        if (valor) puerta.cerrar()
        registrar(if (valor) "Mantenimiento activado; puerta asegurada" else "Mantenimiento desactivado")
        enviarEstado(); observar?.invoke()
    }
    fun cerrarPuerta() { puerta.cerrar(); registrar("Cierre local de la puerta"); enviarEstado(); observar?.invoke() }
    val pendiente get() = solicitudPendiente != null
    val estadoVigente get() = conectado && (esBase || (estadoRemoto != null && SystemClock.elapsedRealtime() - ultimaTelemetria < 5000))

    fun solicitarAcceso() {
        if (!sesion || esBase || !estadoVigente || pendiente) return
        // El ID relaciona esta orden con su respuesta. El reloj mide la ida y vuelta.
        val id = UUID.randomUUID().toString()
        solicitudPendiente = id
        inicioSolicitud = SystemClock.elapsedRealtime()
        enviados++
        conexion.enviar(Tipo.ABRIR, "$sectorElegido|$duracion", id)
        registrar("Solicitud de acceso enviada al sector $sectorElegido", id)
        informar("Solicitud enviada. Esperando autorización de la base…")
    }

    fun consultar() { if (conectado && !esBase) conexion.enviar(Tipo.CONSULTAR) }

    private fun recibirEvento(estado: String, mensaje: Mensaje?) {
        if (!sesion) return
        when (estado) {
            "CONECTADO" -> {
                conectado = true
                ultimoRecibido = SystemClock.elapsedRealtime()
                enlace = "Canal verificado · AES-GCM"
                registrar("Canal de misión autenticado y conectado")
                aviso = "Contacto con la base confirmado. Los aliens pueden esperar."
                if (esBase) enviarEstado() else consultar()
            }
            "ESPERANDO" -> { conectado = false; enlace = "Base escuchando en puerto 8988" }
            "CONECTANDO" -> enlace = "Conectando con la base…"
            "ERROR", "PUERTO_OCUPADO" -> {
                conectado = false
                estadoRemoto = null
                if (solicitudPendiente != null) { agotados++; registrar("Resultado no confirmado por desconexión", solicitudPendiente!!); solicitudPendiente = null }
                enlace = if (estado == "PUERTO_OCUPADO") "No se pudo abrir el puerto 8988" else "Conexión interrumpida o código no verificado"
                aviso = "Sin confirmación de la base. Revisa dirección, código de misión y conexión."
                registrar("Conexión interrumpida o autenticación de par rechazada")
            }
            "MENSAJE" -> mensaje?.let { ultimoRecibido = SystemClock.elapsedRealtime(); procesar(it) }
        }
        observar?.invoke()
    }

    private fun procesar(mensaje: Mensaje) {
        when (mensaje.tipo) {
            Tipo.PING -> conexion.enviar(Tipo.PONG, id = mensaje.id)
            Tipo.PONG -> Unit
            Tipo.CONSULTAR -> if (esBase) enviarEstado()
            Tipo.ABRIR -> if (esBase) {
                // Ejemplo: "2|4" pide el sector 2 durante cuatro segundos.
                val datos = mensaje.datos.split("|")
                val sector = datos.getOrNull(0)?.toIntOrNull() ?: -1
                val segundos = datos.getOrNull(1)?.toIntOrNull() ?: -1
                val resultado = if (datos.size == 2) puerta.abrir(mensaje.id, sector, segundos, SystemClock.elapsedRealtime())
                    else "RECHAZADO|Mensaje de apertura inválido."
                conexion.enviar(Tipo.RESULTADO, resultado, mensaje.id)
                registrar(if (resultado.startsWith("ACEPTADO")) "Acceso autorizado al sector $sector" else "Acceso rechazado por la base", mensaje.id)
                aviso = resultado.substringAfter("|")
                enviarEstado()
            }
            Tipo.RESULTADO -> if (!esBase) {
                if (mensaje.id != solicitudPendiente) { registrar("Respuesta tardía recibida; se consulta el estado", mensaje.id); consultar(); return }
                val partes = mensaje.datos.split("|", limit = 2)
                if (partes.size != 2 || partes[0] !in listOf("ACEPTADO", "RECHAZADO")) { informar("Respuesta inválida de la base."); return }
                val rtt = SystemClock.elapsedRealtime() - inicioSolicitud
                tiempos.add(rtt)
                confirmados++
                solicitudPendiente = null
                aviso = partes[1]
                registrar(if (partes[0] == "ACEPTADO") "Base confirmó acceso autorizado" else "Base confirmó acceso rechazado", mensaje.id, rtt)
            }
            Tipo.ESTADO -> if (!esBase) {
                val datos = mensaje.datos.split("|")
                // Los índices corresponden al orden definido en Puerta.estado().
                if (datos.size != 5 ||
                    datos[0] !in listOf("ABIERTA", "CERRADA") ||
                    datos[1] !in listOf("0", "1") ||
                    datos[2] !in listOf("0", "1") ||
                    datos[3].toIntOrNull() !in 1..AjustesAgencia.sectores.size ||
                    datos[4].toIntOrNull() !in 0..AjustesAgencia.DURACION_MAXIMA
                ) {
                    informar("La base envió un estado inválido."); return
                }
                if (estadoRemoto?.get(0) == "ABIERTA" && datos[0] == "CERRADA") registrar("Base confirmó puerta asegurada")
                estadoRemoto = datos
                ultimaTelemetria = SystemClock.elapsedRealtime()
            }
            else -> Unit
        }
    }

    private fun enviarEstado() { if (conectado && esBase) conexion.enviar(Tipo.ESTADO, puerta.estado(SystemClock.elapsedRealtime())) }

    private fun actualizarReloj() {
        val ahora = SystemClock.elapsedRealtime()
        if (sesion && ahora - ultimaActividad > 300_000) { salir(); informar("Sesión cerrada tras cinco minutos sin actividad.") }
        if (pendiente && ahora - inicioSolicitud >= 5000) {
            agotados++
            registrar("Tiempo de respuesta agotado; apertura no confirmada", solicitudPendiente!!)
            solicitudPendiente = null
            aviso = "La base no respondió a tiempo. Resultado no confirmado."
            consultar()
        }
        if (esBase && puerta.abiertaHasta != 0L && !puerta.abierta(ahora)) {
            puerta.cerrar(); registrar("Cierre automático completado en la base"); enviarEstado()
        }
        if (conectado && ahora - ultimoRecibido > 15_000) {
            desconectar(); informar("La base dejó de responder. Reconecta para consultar el estado.")
        }
        if (conectado && ahora - ultimoLatido >= 2000) {
            ultimoLatido = ahora
            if (esBase) enviarEstado() else conexion.enviar(Tipo.PING)
        }
        observar?.invoke()
    }

    private fun registrar(detalle: String, id: String = "", rtt: Long? = null) {
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        eventos.add(0, Evento(fecha, detalle, id, rtt))
        while (eventos.size > 100) eventos.removeAt(eventos.lastIndex)
        val lista = JSONArray()
        eventos.forEach { e -> lista.put(JSONObject().put("fecha", e.fecha).put("detalle", e.detalle).put("id", e.solicitud).apply { e.rtt?.let { put("rtt", it) } }) }
        preferencias.edit().putString("eventos", lista.toString()).apply()
    }

    fun exportarCsv(): String {
        fun campo(s: String): String {
            // Las comillas CSV no impiden fórmulas en una hoja de cálculo.
            val seguro = if (s.trimStart().firstOrNull() in listOf('=', '+', '-', '@') || s.firstOrNull() in listOf('\t', '\r', '\n')) "'" + s else s
            return "\"${seguro.replace("\"", "\"\"")}\""
        }
        return "fecha,evento,solicitud,rtt_ms\n" + eventos.reversed().joinToString("\n") { e ->
            listOf(e.fecha, e.detalle, e.solicitud, e.rtt?.toString() ?: "").joinToString(",", transform = ::campo)
        }
    }

    override fun onCleared() {
        handler.removeCallbacksAndMessages(null)
        conexion.apagar()
        trabajo.shutdownNow()
    }
}
