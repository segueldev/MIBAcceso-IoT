package cl.seguel.mib

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.PersistableBundle
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.net.NetworkInterface
import kotlin.math.ceil

class MainActivity : ComponentActivity() {
    private lateinit var modelo: ModeloAgencia
    private lateinit var raiz: LinearLayout
    private val actualizar = mutableListOf<() -> Unit>()
    private var pagina = 0
    private var mostrandoSesion = false
    private val verde = Color.rgb(185, 244, 102)
    private val blanco = Color.rgb(239, 242, 234)
    private val gris = Color.rgb(185, 194, 183)
    private val fondo = Color.rgb(12, 16, 13)
    private val sectores = AjustesAgencia.sectores
    private val exportar = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null && modelo.sesion) {
            runCatching { contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(modelo.exportarCsv()) } }
                .onSuccess { modelo.informar("Bitácora exportada. Sin contraseñas ni códigos de misión.") }
                .onFailure { modelo.informar("No se pudo guardar la bitácora en esa ubicación.") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modelo = ViewModelProvider(this)[ModeloAgencia::class.java]
        pagina = savedInstanceState?.getInt("pagina") ?: 0
        modelo.observar = {
            if (mostrandoSesion != modelo.sesion) construir()
            else actualizar.toList().forEach { it() }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (modelo.sesion && pagina != 0) { pagina = 0; construir() }
                else if (modelo.sesion) confirmarSalida() else finish()
            }
        })
        construir()
    }

    override fun onUserInteraction() { super.onUserInteraction(); modelo.actividad() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putInt("pagina", pagina); super.onSaveInstanceState(outState) }
    override fun onDestroy() { modelo.observar = null; super.onDestroy() }

    private fun construir() {
        mostrandoSesion = modelo.sesion
        actualizar.clear()
        raiz = columna().apply { setBackgroundColor(fondo) }
        setContentView(raiz)
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(raiz)
        if (!modelo.sesion) mostrarLogin() else mostrarPanel()
        actualizar.toList().forEach { it() }
    }

    private fun mostrarLogin() {
        val contenido = contenidoDesplazable()
        contenido.addView(texto("MIB  /  ACCESO CLASIFICADO", 12, verde, true).apply { letterSpacing = .12f })
        val foto = MaterialCardView(this).apply {
            radius = dp(18).toFloat(); cardElevation = 0f; clipToOutline = true
            setCardBackgroundColor(fondo); strokeColor = Color.rgb(63, 78, 53); strokeWidth = dp(1)
        }
        foto.addView(ImageView(this).apply {
            setImageResource(R.drawable.agentes_mib)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Tres agentes de Hombres de Negro con sus gafas oscuras"
        }, ViewGroup.LayoutParams(-1, -2))
        contenido.addView(foto, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        val identificacion = fila().apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(14), 0, 0) }
        identificacion.addView(gafas(), LinearLayout.LayoutParams(dp(48), dp(26)))
        identificacion.addView(texto("HOMBRES DE NEGRO", 12, verde, true).apply {
            letterSpacing = .10f; setPadding(dp(10), 0, 0, 0)
        })
        contenido.addView(identificacion)
        contenido.addView(texto("Su identidad, por favor.", 27, blanco, true).apply { setPadding(0, dp(16), 0, dp(8)) })
        contenido.addView(texto("Las gafas dan estilo. La credencial da acceso.", 15, gris))
        val crear = !modelo.credenciales.existe
        val tarjeta = tarjeta(contenido)
        tarjeta.addView(texto(if (crear) "PRIMER INGRESO · CREAR CREDENCIAL" else "IDENTIFICACIÓN DEL AGENTE", 12, verde, true))
        val usuario = campo(tarjeta, "Usuario del agente", R.id.usuario)
        val contrasena = campo(tarjeta, "Contraseña", R.id.contrasena, true)
        val confirmar = if (crear) campo(tarjeta, "Repetir contraseña", R.id.confirmar, true) else null
        tarjeta.addView(texto(if (crear) "Elige tu usuario y una contraseña de al menos 12 caracteres. No hay una clave universal de la agencia." else "Tres intentos fallidos bloquean el ingreso durante 60 segundos.", 13, gris))
        tarjeta.addView(texto("Toca las gafas para mostrar u ocultar la contraseña.", 12, gris).apply { setPadding(0, dp(6), 0, 0) })
        val entrar = boton(tarjeta, if (crear) "CREAR CREDENCIAL E INGRESAR" else "INGRESAR A LA AGENCIA", R.id.ingresar) {
            if (crear && contrasena.text.toString() != confirmar?.text.toString()) { modelo.informar("Las contraseñas no coinciden. Hasta los agentes se equivocan."); return@boton }
            if (contrasena.text.isNullOrEmpty() || usuario.text.isNullOrBlank()) { modelo.informar("Completa el usuario y la contraseña."); return@boton }
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(contrasena.windowToken, 0)
            modelo.autenticar(usuario.text.toString(), contrasena.text.toString().toCharArray(), crear)
            contrasena.text?.clear(); confirmar?.text?.clear()
        }
        val estado = texto("", 14, gris)
        tarjeta.addView(estado)
        actualizar.add {
            val bloqueo = modelo.credenciales.segundosBloqueo()
            entrar.isEnabled = !modelo.ocupado && bloqueo == 0L
            entrar.text = when { modelo.ocupado -> "VERIFICANDO CREDENCIAL…"; bloqueo > 0 -> "BLOQUEADO · ${bloqueo}s"; crear -> "CREAR CREDENCIAL E INGRESAR"; else -> "INGRESAR A LA AGENCIA" }
            estado.text = if (bloqueo > 0) "Demasiados intentos. Vuelve en ${bloqueo}s." else modelo.aviso
        }
        contenido.addView(texto("Proyecto académico inspirado en Men in Black.\nEvolución del control de acceso IoT de la Unidad 1.\nBenjamín Seguel · INACAP Temuco", 12, gris).apply { setPadding(0, dp(20), 0, dp(12)) })
    }

    private fun mostrarPanel() {
        val encabezado = fila().apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(12), dp(12), dp(8)) }
        val marca = columna()
        val nombre = fila().apply { gravity = Gravity.CENTER_VERTICAL }
        nombre.addView(texto("MIB", 30, blanco, true))
        nombre.addView(gafas(), LinearLayout.LayoutParams(dp(44), dp(25)).apply { leftMargin = dp(10) })
        marca.addView(nombre)
        marca.addView(texto("HOMBRES DE NEGRO / ACCESO", 10, verde, true).apply { letterSpacing = .08f })
        encabezado.addView(marca, LinearLayout.LayoutParams(0, -2, 1f))
        val salir = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Salir"; minWidth = dp(64); contentDescription = "Cerrar sesión de agente"
            setOnClickListener { confirmarSalida() }
        }
        encabezado.addView(salir)
        raiz.addView(encabezado)
        val identidad = texto("", 12, gris).apply { setPadding(dp(20), 0, dp(20), dp(8)) }
        raiz.addView(identidad)
        actualizar.add { identidad.text = "AGENTE ${modelo.credenciales.usuario.uppercase()}  /  ${if (modelo.esBase) "TERMINAL BASE" else "OPERADOR"}" }
        val contenido = contenidoDesplazable(true)
        when (pagina) { 0 -> monitoreo(contenido); 1 -> activador(contenido); 2 -> configurarConexion(contenido); 3 -> registro(contenido) }
        val navegacion = fila().apply { setPadding(dp(8), dp(4), dp(8), dp(4)) }
        listOf("Monitoreo", "Activador", "Conexión", "Bitácora").forEachIndexed { indice, etiqueta ->
            val btn = MaterialButton(this, null, if (pagina == indice) com.google.android.material.R.attr.materialButtonStyle else com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                id = listOf(R.id.nav_monitoreo, R.id.nav_activador, R.id.nav_conexion, R.id.nav_registro)[indice]
                text = etiqueta; textSize = 10f; minWidth = 0; minimumWidth = 0
                setPadding(dp(3), 0, dp(3), 0)
                setOnClickListener { pagina = indice; construir() }
            }
            navegacion.addView(btn, LinearLayout.LayoutParams(0, dp(56), 1f))
        }
        raiz.addView(navegacion)
    }

    private fun monitoreo(contenido: LinearLayout) {
        titulo(contenido, "MONITOREO", "La agencia, bajo control.", "Recibe el estado confirmado de la entrada.")
        val estado = tarjeta(contenido, true)
        estado.addView(texto("ENTRADA DE LA AGENCIA", 11, verde, true))
        val puerta = VistaPuerta(this)
        estado.addView(puerta, LinearLayout.LayoutParams(-1, dp(150)))
        val principal = texto("", 26, blanco, true)
        val detalle = texto("", 14, gris)
        estado.addView(principal); estado.addView(detalle)
        val sensores = tarjeta(contenido)
        val presencia = texto("", 16, blanco, true)
        val mantenimiento = texto("", 14, gris)
        val conexion = texto("", 13, verde)
        sensores.addView(presencia); sensores.addView(mantenimiento); sensores.addView(conexion)
        actualizar.add {
            val datos = datosActuales()
            val abierta = datos?.get(0) == "ABIERTA"
            puerta.abierta = abierta; puerta.confirmada = datos != null; puerta.invalidate()
            principal.text = if (datos == null) "ESTADO SIN CONFIRMAR" else if (abierta) "ACCESO ABIERTO" else "PUERTA ASEGURADA"
            detalle.text = if (datos == null) "Conecta con la base para conocer el estado real." else if (abierta) "Cierre automático en ${datos[4]} s. Pase sin tocar nada raro." else "Modo vigilancia. Los secretos siguen adentro."
            presencia.text = if (datos == null) "PIR · Sin datos recientes" else if (datos[1] == "1") "PIR · Presencia detectada" else "PIR · Sin movimiento"
            mantenimiento.text = if (datos == null) "Sector y mantenimiento sin confirmar" else "${sectores[datos[3].toInt() - 1]} · ${if (datos[2] == "1") "Mantenimiento activo" else "Operación normal"}"
            conexion.text = modelo.enlace
        }
        boton(contenido, if (modelo.esBase) "IR AL SIMULADOR DE PUERTA" else "SOLICITAR INGRESO") { pagina = 1; construir() }
        aviso(contenido)
        contenido.addView(texto(if (modelo.esBase) "SIMULACIÓN · Este equipo representa el PIR, la LCD y el servomotor del proyecto original." else "La puerta cambia de estado cuando la base lo confirma. La app no abre una cerradura física.", 12, gris))
    }

    private fun activador(contenido: LinearLayout) {
        titulo(contenido, "ACTIVADOR", if (modelo.esBase) "Terminal de la base." else "Pida permiso, agente.", if (modelo.esBase) "Simulación del hardware de la Unidad 1." else "La base tiene la última palabra. Incluso si lleva lentes oscuros.")
        if (modelo.esBase) {
            val controles = tarjeta(contenido)
            controles.addView(texto("SENSOR Y SEGURIDAD LOCAL", 12, verde, true))
            val presencia = SwitchMaterial(this).apply { id = R.id.presencia; text = "Simular presencia en el sensor PIR"; isChecked = modelo.puerta.presencia; minHeight = dp(56) }
            presencia.setOnCheckedChangeListener { _, valor -> modelo.cambiarPresencia(valor) }
            controles.addView(presencia)
            val mantenimiento = SwitchMaterial(this).apply { id = R.id.mantenimiento; text = "Mantenimiento · impedir aperturas"; isChecked = modelo.puerta.mantenimiento; minHeight = dp(56) }
            mantenimiento.setOnCheckedChangeListener { _, valor -> modelo.cambiarMantenimiento(valor) }
            controles.addView(mantenimiento)
            boton(controles, "ASEGURAR PUERTA AHORA") { modelo.cerrarPuerta() }
            controles.addView(texto("El cierre automático sigue funcionando si el agente se desconecta. El mantenimiento cierra la puerta y rechaza nuevos accesos.", 13, gris))
        } else {
            val controles = tarjeta(contenido)
            controles.addView(texto("DESTINO DE LA SOLICITUD", 12, verde, true))
            val destinos = RadioGroup(this)
            sectores.forEachIndexed { i, nombre ->
                destinos.addView(RadioButton(this).apply { id = 500 + i; text = nombre; minHeight = dp(48); setTextColor(blanco) })
            }
            destinos.check(499 + modelo.sectorElegido)
            destinos.setOnCheckedChangeListener { _, id -> modelo.sectorElegido = id - 499 }
            controles.addView(destinos)
            val duracion = texto("Apertura: ${modelo.duracion} segundos", 14, blanco)
            controles.addView(duracion)
            controles.addView(Slider(this).apply {
                id = R.id.duracion
                valueFrom = AjustesAgencia.DURACION_MINIMA.toFloat()
                valueTo = AjustesAgencia.DURACION_MAXIMA.toFloat()
                stepSize = 1f
                value = modelo.duracion.toFloat()
                contentDescription = "Duración de apertura de ${AjustesAgencia.DURACION_MINIMA} a ${AjustesAgencia.DURACION_MAXIMA} segundos"
                addOnChangeListener { _, valor, _ -> modelo.duracion = valor.toInt(); duracion.text = "Apertura: ${modelo.duracion} segundos" }
            })
            controles.addView(texto("Valor inicial: ${AjustesAgencia.DURACION_INICIAL} segundos. El proyecto original usaba 4 segundos.", 12, gris))
            val solicitar = boton(controles, "SOLICITAR ACCESO", R.id.abrir) { modelo.solicitarAcceso() }
            actualizar.add {
                solicitar.isEnabled = modelo.estadoVigente && !modelo.pendiente
                solicitar.text = if (modelo.pendiente) "ESPERANDO CONFIRMACIÓN…" else "SOLICITAR ACCESO"
            }
            controles.addView(texto("Se requiere presencia en el PIR y que la base esté fuera de mantenimiento. Una orden no se reenvía automáticamente.", 13, gris))
        }
        aviso(contenido)
        val estado = texto("", 16, verde, true)
        contenido.addView(estado)
        actualizar.add { estado.text = datosActuales()?.let { if (it[0] == "ABIERTA") "PUERTA ABIERTA · ${it[4]} s" else "PUERTA CERRADA" } ?: "ESTADO SIN CONFIRMAR" }
    }

    private fun configurarConexion(contenido: LinearLayout) {
        titulo(contenido, "CONEXIÓN", "Contacto con la base.", "Una misma app, dos roles y un código de misión compartido.")
        val roles = tarjeta(contenido)
        roles.addView(texto("FUNCIÓN DE ESTE EQUIPO", 12, verde, true))
        val selector = RadioGroup(this)
        selector.addView(RadioButton(this).apply { id = R.id.rol_agente; text = "Agente · solicitar y monitorear"; minHeight = dp(48); setTextColor(blanco) })
        selector.addView(RadioButton(this).apply { id = R.id.rol_base; text = "Base MIB · simular puerta y sensor"; minHeight = dp(48); setTextColor(blanco) })
        selector.check(if (modelo.esBase) R.id.rol_base else R.id.rol_agente)
        selector.setOnCheckedChangeListener { _, id -> modelo.cambiarRol(id == R.id.rol_base); construir() }
        roles.addView(selector)
        val conexion = tarjeta(contenido)
        if (modelo.esBase) {
            conexion.addView(texto("BASE MIB / PUERTO 8988", 12, verde, true))
            conexion.addView(texto("Direcciones de este equipo:\n${direccionesLocales()}", 14, blanco))
            conexion.addView(texto("En dos teléfonos: conecta ambos a la misma red Wi-Fi privada. En emuladores: sigue los pasos del LEEME.", 13, gris))
            boton(conexion, "MOSTRAR CÓDIGO DE MISIÓN", secundario = true) { mostrarCodigo() }
            boton(conexion, "RENOVAR CÓDIGO", secundario = true) {
                MaterialAlertDialogBuilder(this).setTitle("Renovar código de misión")
                    .setMessage("Se cerrará la conexión. El agente necesitará el nuevo código para volver a ingresar.")
                    .setNegativeButton("Cancelar", null).setPositiveButton("Renovar") { _, _ ->
                        modelo.desconectar()
                        modelo.prepararCodigo(true) { modelo.informar("Código renovado. Comparte el nuevo código con el agente autorizado.") }
                    }.show()
            }
            boton(conexion, "INICIAR BASE MIB", R.id.conectar) { modelo.conectar("", "") }
        } else {
            conexion.addView(texto("DATOS DE LA BASE", 12, verde, true))
            val host = campo(conexion, "Dirección de la base", R.id.direccion).apply { setText(modelo.direccion); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI }
            val codigo = campo(conexion, "Código de misión", R.id.codigo_mision, true).apply { setText(modelo.codigoAgente); setSingleLine(true) }
            conexion.addView(texto("La base genera el código de 43 caracteres. Escríbelo o pégalo aquí; respeta mayúsculas, guiones y guiones bajos.", 13, gris))
            boton(conexion, "CONECTAR CON LA BASE", R.id.conectar) { modelo.conectar(host.text.toString(), codigo.text.toString()) }
        }
        boton(conexion, "DESCONECTAR", secundario = true) { modelo.desconectar(); modelo.informar("Canal cerrado. La base mantiene su propio cierre automático.") }
        val estado = texto("", 14, verde, true)
        conexion.addView(estado)
        actualizar.add { estado.text = modelo.enlace }
        aviso(contenido)
        val ayuda = tarjeta(contenido)
        ayuda.addView(texto("PRUEBA EN EL COMPUTADOR", 12, verde, true))
        ayuda.addView(texto("Abre dos emuladores. El primero será Base MIB y el segundo, Agente. El botón Iniciar base abre el puerto de la simulación. El LEEME explica cómo enlazarlos sin teléfonos físicos.", 14, gris))
    }

    private fun registro(contenido: LinearLayout) {
        titulo(contenido, "BITÁCORA", "Usted no estuvo aquí.", "Pero el registro sí. Últimos 100 eventos de este equipo.")
        val resumen = tarjeta(contenido)
        val estadisticas = texto("", 14, blanco)
        resumen.addView(estadisticas)
        actualizar.add {
            val tiempos = modelo.tiempos.sorted()
            val p95 = if (tiempos.isEmpty()) "—" else "${tiempos[(ceil(tiempos.size * .95).toInt() - 1).coerceAtLeast(0)]} ms"
            val tasa = if (modelo.enviados == 0) "—" else "${modelo.confirmados * 100 / modelo.enviados}%"
            estadisticas.text = "SOLICITUDES DE ESTA EJECUCIÓN\nEnviadas: ${modelo.enviados} · Respondidas: ${modelo.confirmados}\nSin confirmar: ${modelo.agotados} · Respuesta: $tasa\nRTT promedio: ${if (tiempos.isEmpty()) "—" else "${tiempos.average().toLong()} ms"} · P95: $p95"
        }
        boton(resumen, "EXPORTAR BITÁCORA CSV") { exportar.launch("MIB-bitacora.csv") }
        val lista = texto("", 13, gris).apply { setTextIsSelectable(true); setLineSpacing(dp(4).toFloat(), 1f) }
        contenido.addView(lista)
        actualizar.add {
            lista.text = if (modelo.eventos.isEmpty()) "Sin eventos todavía." else modelo.eventos.joinToString("\n\n") { e ->
                "${e.fecha}\n${e.detalle}" + (if (e.solicitud.isEmpty()) "" else "\nID: ${e.solicitud}") + (e.rtt?.let { " · RTT: $it ms" } ?: "")
            }
        }
        aviso(contenido)
    }

    private fun mostrarCodigo() {
        modelo.prepararCodigo(false) { codigo ->
            if (isFinishing || isDestroyed) return@prepararCodigo
            MaterialAlertDialogBuilder(this).setTitle("Código de misión")
                .setMessage("Compártelo solo con el agente autorizado. Quien tenga este código puede identificarse ante la base.\n\n$codigo")
                .setNegativeButton("Cerrar", null).setPositiveButton("Copiar código") { _, _ ->
                    val clip = ClipData.newPlainText("Código de misión", codigo)
                    clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
                    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                    modelo.informar("Código copiado. Pégalo en el equipo del agente.")
                }.show()
        }
    }

    private fun confirmarSalida() {
        MaterialAlertDialogBuilder(this).setTitle("¿Terminar la misión?")
            .setMessage("Se cerrará la sesión y la conexión de este equipo.")
            .setNegativeButton("Seguir aquí", null).setPositiveButton("Cerrar sesión") { _, _ -> modelo.salir() }.show()
    }

    private fun datosActuales(): List<String>? = if (modelo.esBase) modelo.puerta.estado(SystemClock.elapsedRealtime()).split("|") else if (modelo.estadoVigente) modelo.estadoRemoto else null
    private fun direccionesLocales(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .filter { !it.isLoopbackAddress && it.hostAddress?.contains(":") == false }.joinToString("\n") { it.hostAddress ?: "" }.ifEmpty { "No hay IPv4 local disponible." }
    }.getOrDefault("No se pudo consultar la dirección local.")

    private fun contenidoDesplazable(conPeso: Boolean = false): LinearLayout {
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val contenido = columna().apply { setPadding(dp(20), dp(16), dp(20), dp(24)) }
        scroll.addView(contenido)
        raiz.addView(scroll, if (conPeso) LinearLayout.LayoutParams(-1, 0, 1f) else LinearLayout.LayoutParams(-1, -1))
        return contenido
    }
    private fun titulo(padre: LinearLayout, etiqueta: String, titulo: String, descripcion: String) {
        padre.addView(texto(etiqueta, 11, verde, true).apply { letterSpacing = .15f })
        padre.addView(texto(titulo, 28, blanco, true).apply { setPadding(0, dp(8), 0, dp(8)) })
        padre.addView(texto(descripcion, 14, gris).apply { setPadding(0, 0, 0, dp(6)) })
    }
    private fun aviso(padre: LinearLayout) {
        val texto = texto("", 14, gris).apply { setPadding(0, dp(8), 0, dp(16)); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        padre.addView(texto)
        actualizar.add { if (texto.text.toString() != modelo.aviso) texto.text = modelo.aviso }
    }
    private fun campo(padre: LinearLayout, etiqueta: String, identificador: Int, secreto: Boolean = false): TextInputEditText {
        val contenedor = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = etiqueta; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            if (secreto) {
                endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                setEndIconDrawable(R.drawable.ic_gafas)
                setEndIconContentDescription(if (identificador == R.id.codigo_mision)
                    "Mostrar u ocultar código de misión con las gafas del agente"
                    else "Mostrar u ocultar contraseña con las gafas del agente")
                setEndIconTintList(android.content.res.ColorStateList.valueOf(verde))
            }
        }
        val entrada = TextInputEditText(contenedor.context).apply {
            id = identificador; setTextColor(blanco); textSize = 16f; setSingleLine(true)
            inputType = if (secreto) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            isSaveEnabled = !secreto
            filters = arrayOf(android.text.InputFilter.LengthFilter(when (identificador) {
                R.id.usuario -> 24
                R.id.codigo_mision -> 43
                R.id.direccion -> 253
                else -> 128
            }))
            setPadding(dp(14), dp(18), dp(14), dp(18))
        }
        contenedor.addView(entrada, LinearLayout.LayoutParams(-1, -2))
        padre.addView(contenedor, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14); bottomMargin = dp(4) })
        return entrada
    }
    private fun tarjeta(padre: LinearLayout, destacada: Boolean = false): LinearLayout {
        val tarjeta = MaterialCardView(this).apply {
            radius = dp(18).toFloat(); cardElevation = 0f
            setCardBackgroundColor(if (destacada) Color.rgb(24, 36, 21) else Color.rgb(23, 28, 24))
            strokeColor = if (destacada) Color.rgb(85, 115, 56) else Color.rgb(52, 63, 51); strokeWidth = dp(1)
        }
        val interior = columna().apply { setPadding(dp(18), dp(20), dp(18), dp(20)) }
        tarjeta.addView(interior)
        padre.addView(tarjeta, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16); bottomMargin = dp(4) })
        return interior
    }
    private fun boton(padre: LinearLayout, etiqueta: String, identificador: Int = View.NO_ID, secundario: Boolean = false, accion: () -> Unit): MaterialButton {
        val boton = MaterialButton(this, null, if (secundario) com.google.android.material.R.attr.materialButtonOutlinedStyle else com.google.android.material.R.attr.materialButtonStyle).apply {
            id = identificador; text = etiqueta; textSize = 12f; isAllCaps = false
            minHeight = dp(52); cornerRadius = dp(12)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener { accion() }
        }
        padre.addView(boton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        return boton
    }
    private fun texto(contenido: String, tamano: Int, color: Int, negrita: Boolean = false) = TextView(this).apply {
        text = contenido; textSize = tamano.toFloat(); setTextColor(color)
        if (negrita) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(2).toFloat(), 1f)
    }
    private fun columna() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun fila() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    private fun gafas() = ImageView(this).apply {
        setImageResource(R.drawable.ic_gafas)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()
}
