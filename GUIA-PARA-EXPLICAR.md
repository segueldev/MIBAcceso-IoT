# Entender y practicar con MIB Acceso

Esta guía sirve para estudiar el código y preparar la demostración de la formativa. Completa los resultados después de hacer cada ejercicio. Si algo falla, registra lo que viste y la corrección que realmente probaste.

## 1. Qué viene del trabajo anterior

En la Evaluación 1, Benjamín Seguel y Marcelo Ducommun trabajaron con un PIR, teclado, LCD y servomotor. Al ingresar el PIN correcto, el servo abría durante cuatro segundos. El [programa Arduino original](antecedentes/Unidad1/ControlAccesoOriginal.ino) conserva ese antecedente.

La app continúa el problema de control de acceso. La temática de **Hombres de Negro / Men in Black** aparece en los agentes, los sectores, la foto, las gafas y los mensajes. El humor acompaña la función: un agente debe identificarse y pedir permiso para entrar.

En esta versión hay **una puerta simulada y tres sectores de destino**: Archivo alienígena, Laboratorio y Sala del neuralizador. Elegir un sector cambia el destino de la solicitud; no crea tres cerraduras independientes. La Base MIB representa el hardware y el Agente solicita acceso desde otra instancia de la misma app.

## 2. Preparar la práctica

Sigue primero los pasos de [conexión entre dos emuladores](README.md). Crea una credencial local en cada app, inicia una como Base MIB y conecta la otra como Agente usando el código de misión de la base. Los usuarios pueden ser distintos.

Mantén las dos apps abiertas. En la base, deja mantenimiento desactivado. Usa el interruptor de presencia para representar el PIR. En el agente, deja inicialmente cuatro segundos y el primer sector. Revisa Monitoreo antes de solicitar acceso.

## 3. Seguir una solicitud de principio a fin

1. En [MainActivity.kt](app/src/main/java/cl/seguel/mib/MainActivity.kt), busca `activador()`. El botón **Solicitar acceso** llama a `modelo.solicitarAcceso()`.
2. En [ModeloAgencia.kt](app/src/main/java/cl/seguel/mib/ModeloAgencia.kt), `solicitarAcceso()` comprueba que haya sesión, datos recientes y ninguna solicitud pendiente. Crea un ID único y guarda el instante de envío.
3. `conexion.enviar(Tipo.ABRIR, ...)` prepara los datos. Para sector 1 y cuatro segundos, el contenido es `1|4`. El carácter `|` separa los dos valores.
4. En [ConexionWifi.kt](app/src/main/java/cl/seguel/mib/ConexionWifi.kt), `enviar()` entrega el mensaje al canal. En [CanalSeguro.kt](app/src/main/java/cl/seguel/mib/CanalSeguro.kt), `CanalSeguro.enviar()` añade sesión y secuencia; `Cifrado.escribir()` cifra y envía los bytes.
5. En la base, `CanalSeguro.recibir()` comprueba el mensaje. `ModeloAgencia.procesar()` separa los datos de `ABRIR` y llama a `Puerta.abrir()`.
6. En [Puerta.kt](app/src/main/java/cl/seguel/mib/Puerta.kt), `abrir()` revisa sector, duración, mantenimiento, presencia y si la puerta ya estaba abierta. Si acepta, calcula cuándo debe cerrar.
7. La base envía `RESULTADO` con el mismo ID de la solicitud. Después, `enviarEstado()` informa la situación de la puerta mediante `ESTADO`.
8. El agente procesa la respuesta, calcula el tiempo de ida y vuelta y actualiza los datos que ve la pantalla. `actualizarReloj()` en la base atiende el cierre automático.

Pulsar el botón no dibuja inmediatamente una puerta abierta. La pantalla espera el estado recibido de la base. Si la respuesta no llega a tiempo, el resultado queda **sin confirmar**; la orden no se repite automáticamente.

## 4. Entender los mensajes

Estos ejemplos muestran el contenido antes de cifrarlo. Por la conexión se envía una trama cifrada que también contiene tipo, ID, sesión y secuencia.

| Tipo | Ejemplo de datos | Significado |
| --- | --- | --- |
| `ABRIR` | `1\|4` | Solicitar sector 1 durante cuatro segundos. |
| `RESULTADO` | `ACEPTADO\|Acceso autorizado. Bienvenido a MIB.` | La base aceptó la solicitud identificada por ese ID. |
| `ESTADO` | `CERRADA\|1\|0\|2\|0` | Puerta cerrada, presencia activa, mantenimiento desactivado, sector 2 y cero segundos restantes. |

`Puerta.estado()` arma los cinco campos de estado en ese orden. `ModeloAgencia.procesar()` los valida y `MainActivity.datosActuales()` los entrega a la pantalla. El sector corresponde al último destino aceptado; no cambia por seleccionar una opción que la base todavía no ha autorizado.

## 5. Tres elementos de seguridad diferentes

| Elemento | Quién lo maneja | Para qué sirve |
| --- | --- | --- |
| Contraseña local | La persona que crea la cuenta en cada app. | Permite entrar a esa app. No se envía al otro dispositivo. |
| Código de misión | La base lo genera y se comparte con el agente autorizado. | Es una clave aleatoria compartida para verificar y cifrar la conexión. |
| Clave de Android Keystore | La administra Android para la app de la base. | Protege el código de misión cuando se guarda localmente. No se copia al agente. |

En [Credenciales.kt](app/src/main/java/cl/seguel/mib/Credenciales.kt), `crear()` guarda una sal aleatoria y un verificador obtenido mediante PBKDF2. `verificar()` deriva de nuevo el valor y lo compara. La sal hace que dos contraseñas iguales no tengan necesariamente el mismo verificador. Las iteraciones aumentan el trabajo de probar contraseñas.

AES-GCM, usado en `Cifrado`, protege los mensajes y detecta cambios en su contenido. Las funciones criptográficas provienen de las bibliotecas de Java y Android. El formato de mensajes y el saludo de conexión pertenecen a este prototipo académico.

El código de misión autoriza a quien lo tenga. No hay un servidor central que compruebe una identidad individual de agente. Por eso se comparte solo con el equipo autorizado y puede renovarse desde la base.

## 6. Tiempos que conviene ubicar

| Tiempo o límite | Función | Dónde buscar |
| --- | --- | --- |
| 4 segundos iniciales; rango de 1 a 10 | Duración solicitada de apertura. | Constantes de [AjustesAgencia.kt](app/src/main/java/cl/seguel/mib/AjustesAgencia.kt), usadas por el modelo, el slider y `Puerta.abrir()`. |
| Revisión cada 1 segundo | Revisa cierre, sesión y plazos pendientes. | Inicio del modelo y `actualizarReloj()`. |
| 2 segundos | La base actualiza el estado; el agente envía un mensaje de actividad. | `actualizarReloj()`. |
| 5 segundos | Espera de conexión/saludo, respuesta de apertura y vigencia de datos remotos. Son comprobaciones distintas. | `ConexionWifi.iniciar()`, `CanalSeguro.abrir()`, `estadoVigente` y `actualizarReloj()`. |
| 15 segundos | Detecta una conexión que dejó de entregar mensajes. | `CanalSeguro.abrir()` y `actualizarReloj()`. |
| 60 segundos tras 3 fallos | Bloqueo temporal del login. | `Credenciales.verificar()` y `segundosBloqueo()`. |
| 5 minutos sin interacción | Cierre de la sesión local. El tráfico de red no cuenta como interacción. | `ModeloAgencia.actividad()` y `actualizarReloj()`. |

El cierre se calcula con `SystemClock.elapsedRealtime()`, que mide tiempo transcurrido sin depender de ajustar la hora del equipo. La revisión y la actualización de pantalla son periódicas: no deben interpretarse como un control industrial de precisión. El bloqueo del login usa la hora del equipo para persistir entre aperturas de la app; esa limitación está indicada en el README.

## 7. Por qué hay hilos, Handler y una generación

El hilo principal atiende la pantalla. Si se quedara esperando una conexión o calculando la contraseña, los controles podrían dejar de responder. `ConexionWifi.iniciar()` usa otro hilo para la red; `ModeloAgencia.autenticar()` usa un ejecutor para el trabajo de credenciales.

Un ejecutor organiza tareas para que se realicen fuera de la pantalla. El ejecutor de escritura de `ConexionWifi` mantiene ordenados los envíos. `Handler(Looper.getMainLooper())` permite volver al hilo principal para actualizar el modelo y avisar a la interfaz.

La **generación** es un número de intento de conexión. Al cerrar una conexión, aumenta. Si un hilo anterior entrega una respuesta después de iniciar otra conexión, se descarta porque corresponde a otra generación. `versionSesion` aplica una idea parecida cuando se consulta el código de misión: cerrar sesión invalida una respuesta pendiente.

## 8. Ideas que debes poder demostrar

- **Resultado y estado:** aceptar una orden y mostrar la puerta abierta son mensajes relacionados, pero distintos.
- **ID, sesión y secuencia:** el ID relaciona solicitud y respuesta; la sesión identifica la conexión verificada; la secuencia ordena sus mensajes y permite rechazar repeticiones.
- **Orden duplicada:** `Puerta.abrir()` recuerda los resultados de las últimas 256 solicitudes. Recibir de nuevo uno de esos ID no vuelve a abrir la puerta.
- **RTT:** es el tiempo entre enviar la solicitud y recibir su resultado, medido por el agente. Una solicitud rechazada también puede tener una respuesta válida y un RTT.
- **Datos antiguos:** si dejan de llegar estados recientes, el agente muestra falta de confirmación en vez de asegurar que sabe cómo está la puerta.
- **Límite de la simulación:** cerrar la puerta de la pantalla no demuestra que un servo físico haya cerrado.

## 9. Cambios pequeños delante del profesor

Abre primero [AjustesAgencia.kt](app/src/main/java/cl/seguel/mib/AjustesAgencia.kt). Reúne los valores y mensajes que puedes personalizar sin buscar entre todas las pantallas. En Android Studio, busca ese nombre de archivo y déjalo abierto junto a `Puerta.kt` y `MainActivity.kt`.

| Si pide cambiar… | Archivo y nombre que debes buscar | Qué probar después |
| --- | --- | --- |
| La apertura inicial a 6 segundos | `AjustesAgencia.kt` → `DURACION_INICIAL = 6`. El modelo toma ese valor al crearse. | Reinicia la ejecución; Activador debe comenzar en 6. Solicita acceso y observa el cierre. |
| El máximo a 8 segundos | `AjustesAgencia.kt` → `DURACION_MAXIMA = 8`. Lo usan el slider y la validación de `Puerta.abrir()`. | El slider debe detenerse en 8. Comprueba una apertura de 8 segundos. |
| El nombre de un sector | `AjustesAgencia.kt` → `sectores`. Por ejemplo, reemplaza “Laboratorio” por “Laboratorio de aliens”, conservando el orden. | Elige ese destino, solicita acceso y comprueba el nombre en Monitoreo. |
| El mensaje al permitir acceso | `AjustesAgencia.kt` → `ACCESO_AUTORIZADO`. | Activa presencia, desactiva mantenimiento y solicita acceso. Debe verse el nuevo mensaje. |
| El mensaje de rechazo | `AjustesAgencia.kt` → `SIN_PRESENCIA` o `EN_MANTENIMIENTO`. | Provoca la condición correspondiente y observa la respuesta. |
| La foto, las gafas o un texto del inicio | `MainActivity.kt` → `mostrarLogin()` y `campo()`. | Abre el inicio y revisa la imagen, el texto o el control que cambiaste. |
| La regla de presencia | `Puerta.kt` → `abrir()`. Primero explica el `when` y la condición `!presencia`. | Demuestra rechazo sin presencia y aceptación con presencia; no elimines la regla para cambiar un texto. |

Cambia una cosa a la vez. Conserva `0 < DURACION_MINIMA < DURACION_MAXIMA` y el valor inicial dentro del rango, incluidos sus extremos. Si cambias valores o nombres, compila e instala la misma versión en ambos emuladores; detén y vuelve a iniciar la app para que el modelo tome los nuevos valores. No hace falta borrar las credenciales.

Antes de cada cambio, apunta el valor anterior. Después de probarlo, decide si lo conservas o lo restauras. Para comprobar compilación y pruebas, usa `./gradlew :app:assembleDebug :app:testDebugUnitTest`; revisa cualquier fallo antes de continuar.

**Mi cambio:** ____ · **Valor anterior/nuevo:** ____ · **Resultado de compilación:** ____ · **Qué observé en ambos equipos:** ____ · **Lo conservé o lo restauré:** ____.

## 10. Prácticas para completar con tus resultados

Anota fecha, cambio realizado y evidencia. Los resultados de esta tabla se completan después de practicar; no son pruebas ya ejecutadas por el estudiante.

| Práctica | Pasos | Resultado personal |
| --- | --- | --- |
| Presencia | Con ambos equipos conectados, desactiva presencia en la base y solicita acceso. Actívala y solicita otra vez. | Fecha: ____ · Mensajes observados: ____ |
| Mantenimiento | Activa presencia y mantenimiento en la base; solicita acceso. Desactiva mantenimiento y repite. | Qué cambió y por qué: ____ |
| Cierre y desconexión | Solicita el máximo configurado (10 segundos en la entrega). Con la puerta abierta, desconecta al agente desde Conexión. Mira el cierre en la base y luego reconecta. | Estado en cada equipo y tiempo observado: ____ |
| Sector y duración | Selecciona Laboratorio y 6 segundos, solicita acceso y revisa Monitoreo. Explica qué campos cambiaron. | Campos identificados: ____ |
| Un mensaje propio | Cambia `SIN_PRESENCIA` en `AjustesAgencia.kt`. Compila, instala la misma versión en ambos equipos y provoca ese rechazo. | Texto elegido, archivo y resultado: ____ |
| Bloqueo del login | Guarda tu credencial de práctica. Cierra sesión, ingresa una contraseña incorrecta tres veces y observa el bloqueo. Espera y vuelve a entrar con la correcta. | Duración y comportamiento observados: ____ |
| Tiempo de respuesta | Haz cinco solicitudes, esperando el cierre entre ellas, y revisa Bitácora. Exporta el CSV y distingue accesos aceptados de respuestas recibidas. | Cinco RTT y observación: ____ |

Si aparece un problema, completa: **acción realizada:** ____; **resultado esperado:** ____; **resultado observado:** ____; **corrección probada:** ____; **qué pasó al repetir:** ____. Si no hubo un fallo, escribe eso; no necesitas inventar uno.

## 11. Revisar pruebas y alcance antes de presentar

Lee [PuertaTest.kt](app/src/test/java/cl/seguel/mib/PuertaTest.kt) para estudiar presencia, cierre y duplicados; [CredencialesTest.kt](app/src/test/java/cl/seguel/mib/CredencialesTest.kt) para login y bloqueo; [CanalSeguroTest.kt](app/src/test/java/cl/seguel/mib/CanalSeguroTest.kt) para cifrado, tramas y repetición; e [InicioTest.kt](app/src/test/java/cl/seguel/mib/InicioTest.kt) para el formulario inicial.

Desde la terminal del proyecto puedes ejecutar `./gradlew :app:testDebugUnitTest`. El [README](README.md) explica los requisitos de compilación. Registra tu ejecución: **fecha:** ____; **resultado:** ____; **dudas que surgieron:** ____.

Las pruebas de dos emuladores verifican comunicación por una red virtual. El reenvío ADB permite enlazarlos desde el computador; no mide alcance ni interferencias de Wi-Fi físico. La aplicación todavía no controla el Arduino original. Mantener ambas apps abiertas es parte de la demostración: la base no es un servicio industrial en segundo plano.

Quedan por validar la demostración con equipos reales, la aceptación del alcance por el docente y la publicación del enlace real de GitHub solicitado para la entrega. Las referencias ISO y OT orientan los controles del laboratorio; el proyecto no tiene una certificación ISO ni acredita una instalación OT completa.
