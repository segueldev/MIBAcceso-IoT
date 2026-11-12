# Sumativa TI3042 Unidad 2

[Informe y rúbrica](docs/SUMATIVA-U2.md) · [Demostración rápida](docs/DEMO-RAPIDA.md) · [Mockup](docs/MOCKUP.svg)

**Actualización 12 de noviembre de 2026:** autenticación y eventos ahora persisten en SQLite con migración desde las preferencias de la versión anterior. Las referencias a SharedPreferences en la explicación histórica corresponden a esa versión. Resultados actuales: docs/PRUEBAS-ACTUALES.json.

# MIB Acceso a la Agencia

Sumativa de la Unidad 2 de Aplicaciones Móviles para IoT, TI3042. Estudiante: Benjamín Seguel. Antecedente de la Unidad 1: sistema de control de acceso de Benjamín Seguel y Marcelo Ducommun, INACAP Temuco.

La temática está inspirada explícitamente en la película **Hombres de Negro (Men in Black)**: identificarse como agente, solicitar acceso al cuartel y consultar una bitácora de actividad. Las referencias y el humor acompañan funciones de seguridad que se pueden probar.

El inicio incorpora la fotografía de los agentes proporcionada por el estudiante. Se muestra completa, con sus proporciones originales. Las gafas aparecen en la cabecera y en los campos secretos: al tocarlas se muestra u oculta la contraseña o el código de misión.

## Qué evoluciona del trabajo anterior

El documento original incluye Arduino Uno, sensor PIR, teclado 4×4, LCD I2C y servomotor. Con un PIN correcto, el servo abre durante cuatro segundos y vuelve al modo de vigilancia. La salida Serial se planteó como preparación para la comunicación móvil de las siguientes unidades.

La app Android conserva ese caso de uso. Un equipo **Base MIB** simula el PIR, la LCD y la cerradura; otro, **Agente**, consulta el estado y solicita abrir. El valor inicial de apertura es de cuatro segundos. El código Arduino extraído del documento está en `antecedentes/Unidad1/ControlAccesoOriginal.ino`, con su autoría conservada. No se afirma que la app controle ese Arduino: su integración física es una etapa posterior.

El informe de Marcelo sobre Umbral se consultó para orientar la evolución. No se recibió el código Android de Umbral; esta implementación Android se construyó a partir de los requisitos y el antecedente Arduino documentado.

## Abrir y compilar

1. En Android Studio, abre la carpeta `MIBAcceso` como proyecto.
2. Usa JDK 21 para Gradle, Android SDK 36 y Build Tools 35.0.0. Android Studio puede solicitar instalar esos paquetes.
3. Sincroniza el proyecto y ejecuta la configuración `app`. Requiere Android 8.0 o posterior.

También se puede comprobar desde la terminal del proyecto:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

La APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. Es una compilación de depuración para el laboratorio. No hay una contraseña ni código de misión universales dentro de la aplicación.

## Prueba con dos emuladores

En el computador de este trabajo quedaron preparados dos AVD de Android 15, llamados `MIB_Base` y `MIB_Agente`, en la carpeta auxiliar de la formativa. El archivo `herramientas/iniciar-demo-linux.sh` puede abrirlos sin borrar sus datos. En otro computador, créalos desde Device Manager de Android Studio, instala la misma APK en ambos y sigue estos pasos:

1. Crea una credencial en cada app. El usuario puede ser diferente: es una cuenta local del dispositivo. La contraseña debe tener entre 12 y 128 caracteres.
2. En el primer emulador entra en **Conexión**, elige **Base MIB** y pulsa **Iniciar base MIB**.
3. En la terminal del computador, consulta los identificadores de los emuladores y reenvía el puerto del que actúa como base:

```bash
adb devices
adb -s emulator-5554 forward tcp:8988 tcp:8988
```

Reemplaza `emulator-5554` si tu base tiene otro identificador. Este reenvío es para el laboratorio virtual, no para teléfonos físicos.

4. En Base MIB pulsa **Mostrar código de misión**. Comparte el código únicamente con el otro equipo autorizado.
5. En el segundo emulador elige **Agente**. Usa la dirección `10.0.2.2`, escribe o pega el código completo de 43 caracteres y pulsa **Conectar con la base**.
6. En la base abre **Activador** y activa **Simular presencia en el sensor PIR**.
7. En el agente abre **Activador**, conserva los cuatro segundos y pulsa **Solicitar acceso**. La base autoriza la apertura y después informa el cierre.
8. Prueba sin presencia, con mantenimiento activo y desconectando al agente durante una apertura. El agente debe mostrar falta de confirmación cuando pierde el enlace; la base debe cerrar por su cuenta.

Puedes tener las dos ventanas lado a lado. `adb` está en la carpeta `platform-tools` del SDK; si no está en el PATH, usa su ruta completa. Para cerrar los emuladores de esta práctica puedes usar el botón de cierre de sus ventanas.

Mantén ambas apps abiertas durante la demostración. La base es un simulador en la aplicación, no un servicio industrial en segundo plano. Si Android termina su proceso, la simulación vuelve a puerta cerrada al reiniciar; esto no constituye una garantía para un actuador físico.

## Prueba futura con teléfonos y Wi-Fi

Instala la APK en dos Android y conecta ambos a la misma red Wi-Fi privada. La base muestra su dirección IPv4 en Conexión; usa esa dirección en el agente. El código de misión se comparte fuera del canal inicial. No uses `10.0.2.2` ni el reenvío ADB en este escenario. Algunas redes de invitados aíslan sus equipos e impiden la conexión local.

Las pruebas de emuladores demuestran la lógica y comunicación entre dos instancias Android por una red virtual. No acreditan alcance, interferencia ni estabilidad de una radio Wi-Fi física. La demostración con equipos reales y la aceptación del alcance por el docente quedan pendientes.

## Repetir la prueba de retardo controlado

Desconecta primero al agente. Para añadir una demora real al trayecto reenviado por ADB, cambia el puerto del reenvío y ejecuta el relay de laboratorio:

```bash
adb -s emulator-5554 forward --remove tcp:8988
adb -s emulator-5554 forward tcp:18988 tcp:8988
python3 herramientas/relay-retardo.py --retardo-ms 150
```

Vuelve a conectar al agente con la misma dirección `10.0.2.2`. El relay escucha solo en el computador local y retrasa 150 ms cada trama en cada sentido; no descifra los mensajes. Realiza diez solicitudes, espera el cierre entre ellas y registra sus RTT. La base debe mantenerse atendida durante la prueba para que no venza su sesión por inactividad.

Al terminar, desconecta al agente, cierra el relay con Ctrl+C y restaura el reenvío normal:

```bash
adb -s emulator-5554 forward --remove tcp:18988
adb -s emulator-5554 forward tcp:8988 tcp:8988
```

En este trabajo se usaron 20 solicitudes normales y 10 con ese retardo. La configuración de demora UMTS del emulador no afectó el trayecto ADB y por eso no se utilizó para afirmar un ensayo con latencia añadida.

## Seguridad implementada

- Alta inicial sin PIN fijo; contraseña local derivada mediante PBKDF2-HMAC-SHA256, sal aleatoria y 210.000 iteraciones. No se guarda la contraseña en texto.
- Tres fallos de login producen un bloqueo de 60 segundos guardado en almacenamiento privado. El vencimiento usa la hora del equipo; no es resistente a manipulación administrativa del reloj o borrado de datos.
- Sesión local en memoria, cierre manual y cierre tras cinco minutos sin interacción. La actividad de la red no mantiene iniciada una sesión desatendida.
- Código de misión aleatorio de 256 bits, protegido en la base mediante una clave AES de Android Keystore. En el agente se mantiene en memoria y se descarta al cerrar sesión.
- Mensajes con AES-256-GCM, IV aleatorio de 96 bits, etiqueta de integridad y contexto de protocolo. Hay saludo de desafío, sesión nueva y secuencias estrictas para rechazar repeticiones de tramas.
- Longitud máxima de trama de 8192 bytes, lecturas completas, tiempos de espera y operaciones de red fuera del hilo de interfaz.
- La base valida sector, duración, presencia y mantenimiento. Conserva los resultados de las últimas 256 solicitudes para no ejecutar dos veces el mismo ID. No se reenvían aperturas automáticamente.
- La bitácora conserva hasta 100 eventos y no incluye contraseñas ni códigos. El usuario decide dónde exportarla. Las copias automáticas y transferencia de datos de la app se excluyen.

El código de misión autoriza a quien lo posee; no es una identidad individual de agente verificada por un servidor central. Al renovarlo se revoca el código anterior. El protocolo de aplicación es académico y requiere revisión independiente antes de uso real. `usesCleartextTraffic=false` no protege por sí solo los sockets: la protección aquí la aplica `CanalSeguro`.

## Organización del código

Para empezar a modificarlo, abre [AjustesAgencia.kt](app/src/main/java/cl/seguel/mib/AjustesAgencia.kt). Allí están los segundos de apertura, los nombres de los sectores y tres mensajes de respuesta. La pantalla y la base leen los mismos valores: cambiar el máximo modifica tanto el slider como la validación de las solicitudes. Instala la misma versión en los dos equipos después de cambiarlo.

La [guía de práctica](GUIA-PARA-EXPLICAR.md) incluye cambios pequeños, las funciones que debes ubicar y qué observar después. Para un cambio de reglas, continúa con `Puerta.kt`; para cambiar las pantallas, con `MainActivity.kt`.

| Archivo | Responsabilidad |
| --- | --- |
| `AjustesAgencia.kt` | Tiempos de apertura, nombres de destinos y mensajes que se pueden modificar para practicar. |
| `MainActivity.kt` | Pantallas, navegación y controles Material Design. |
| `ModeloAgencia.kt` | Sesión, estados, respuestas, temporizadores y bitácora. |
| `Puerta.kt` | Condiciones de apertura y cierre en la base. |
| `Credenciales.kt` | Contraseñas, bloqueo y protección local del código de misión. |
| `ConexionWifi.kt` | Cliente y servidor TCP local, lectura y cierre de conexiones. |
| `CanalSeguro.kt` | Cifrado autenticado, saludo de misión y validación de secuencias. |
| `VistaPuerta.kt` | Dibujo propio de la puerta de la agencia. |

## Entrega de la sumativa

Revisa el informe y las evidencias de la carpeta `entrega`. Las mediciones deben conservar el contexto de emulación. Para AAI, la guía solicita un archivo `.txt` con la URL real de GitHub del código. Todavía se debe publicar el repositorio y comprobar el acceso del docente. No se incluye un enlace ficticio.

Antes de subir el código, excluye `local.properties`, `.gradle`, `.idea`, `.kotlin`, `build`, códigos de misión y las herramientas auxiliares de emulación. El `.gitignore` del proyecto ya excluye los archivos locales de compilación. Conserva las pruebas, las instrucciones y el antecedente Arduino con sus autores.

## Fuentes técnicas

- Guía TI3042 Unidad 2 y rúbrica formativa entregadas por el estudiante.
- Seguel, B. y Ducommun, M. Sistema de Control de Acceso Automatizado con Monitoreo, documento de la Unidad 1.
- Android Developers: [criptografía](https://developer.android.com/privacy-and-security/cryptography), [Android Keystore](https://developer.android.com/privacy-and-security/keystore), [redes del emulador](https://developer.android.com/studio/run/emulator-networking).
- [ISO/IEC 27400:2022](https://www.iso.org/standard/44373.html): orientación sobre riesgos y controles de seguridad y privacidad IoT.
- [ISO/IEC 27001:2022](https://www.iso.org/standard/27001): referencia de gestión de riesgos de seguridad de la información.
- [NIST SP 800-82 Rev. 3](https://csrc.nist.gov/pubs/sp/800/82/r3/final): referencia para analizar disponibilidad, fiabilidad y seguridad operacional en OT.

Los controles se aplican al prototipo de laboratorio. Este trabajo no declara una certificación ISO ni una conformidad integral de una instalación OT.
