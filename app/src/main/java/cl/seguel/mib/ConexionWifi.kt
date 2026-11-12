package cl.seguel.mib

import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class ConexionWifi(private val evento: (Int, String, Mensaje?) -> Unit) {
    // Contador para controlar reconexiones y no mezclar mensajes antiguos
    private val contador = AtomicInteger()
    val generacion get() = contador.get()
    // Executor de un solo hilo para enviar los paquetes en orden
    private val escritura = Executors.newSingleThreadExecutor()
    @Volatile private var servidor: ServerSocket? = null
    @Volatile private var socketPendiente: Socket? = null
    @Volatile private var canal: CanalSeguro? = null

    fun iniciar(esBase: Boolean, direccion: String, codigo: String) {
        cerrar()
        val turno = generacion
        Thread({
            var escucha: ServerSocket? = null
            try {
                if (esBase) {
                    escucha = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(8988)) }
                    synchronized(this) {
                        if (turno != generacion) { escucha.close(); return@Thread }
                        servidor = escucha
                    }
                }
                do {
                    var conexion: CanalSeguro? = null
                    var socket: Socket? = null
                    try {
                        emitir(turno, if (esBase) "ESPERANDO" else "CONECTANDO")
                        socket = if (esBase) escucha!!.accept() else Socket()
                        synchronized(this) {
                            if (turno != generacion) { socket.close(); return@Thread }
                            socketPendiente = socket
                        }
                        if (!esBase) socket.connect(InetSocketAddress(direccion, 8988), 5000)
                        conexion = CanalSeguro.abrir(socket, codigo, esBase)
                        synchronized(this) {
                            if (turno != generacion) { conexion.cerrar(); return@Thread }
                            canal = conexion
                        }
                        emitir(turno, "CONECTADO")
                        while (turno == generacion) emitir(turno, "MENSAJE", conexion.recibir())
                    } catch (_: Exception) {
                        emitir(turno, "ERROR")
                    } finally {
                        conexion?.cerrar()
                        runCatching { socket?.close() }
                        synchronized(this) { if (turno == generacion) { canal = null; socketPendiente = null } }
                    }
                } while (esBase && turno == generacion)
            } catch (_: Exception) {
                emitir(turno, "PUERTO_OCUPADO")
            } finally { runCatching { escucha?.close() } }
        }, "MIB-red").start()
    }

    fun enviar(tipo: Tipo, datos: String = "", id: String = java.util.UUID.randomUUID().toString()) {
        val turno = generacion
        val destino = canal ?: return
        escritura.execute {
            if (turno == generacion) try { destino.enviar(tipo, datos, id) }
            catch (_: Exception) { destino.cerrar(); emitir(turno, "ERROR") }
        }
    }

    private fun emitir(turno: Int, estado: String, mensaje: Mensaje? = null) {
        if (turno == generacion) evento(turno, estado, mensaje)
    }

    @Synchronized fun cerrar() {
        contador.incrementAndGet()
        runCatching { servidor?.close() }
        runCatching { socketPendiente?.close() }
        canal?.cerrar()
        canal = null; servidor = null; socketPendiente = null
    }

    fun apagar() { cerrar(); escritura.shutdownNow() }
}
