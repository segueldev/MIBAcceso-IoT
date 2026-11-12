# Demostración y entrega

## Entrega inmediata

Subir a AAI `Entrega_AAI_TI3042_Benjamin_Seguel.txt`. El enlace debe abrir el código del repositorio. La guía señala evaluación individual; se conserva a Marcelo como coautor del antecedente Arduino, sin atribuir una entrega individual a ambos.

## Dos teléfonos Android

1. Instalar `MIBAcceso-Sumativa.apk` en ambos (Android 8 o posterior). Es APK de laboratorio. Si una instalación anterior da conflicto de firma, desinstalarla antes solo después de exportar sus eventos: desinstalar borra los datos.
2. Conectarlos a la misma Wi-Fi privada. No se requiere internet, pero la red debe permitir tráfico entre equipos.
3. Crear cuenta local en cada dispositivo con usuario de 3 a 24 caracteres y contraseña de al menos 12. No se necesita la misma cuenta en ambos.
4. En Conexión del primer teléfono seleccionar Base e iniciar. Mostrar su código de misión y compartirlo únicamente con el segundo equipo autorizado.
5. En el segundo seleccionar Agente, introducir la IPv4 que muestra la Base y su código completo. Conectar. En teléfonos no usar 10.0.2.2.
6. En Activador de Base activar presencia y dejar mantenimiento apagado.
7. En Activador del Agente solicitar acceso por cuatro segundos. Observar confirmación, apertura en Base y cierre automático.
8. Desactivar presencia y repetir: debe rechazar. Activar mantenimiento y repetir: debe rechazar.
9. Consultar Bitácora y exportar CSV. Desconectar Agente durante apertura: Base debe cerrar por su temporizador.
10. Cerrar sesión y mostrar tres logins incorrectos: bloqueo temporal. No publicar la contraseña ni el código de misión en evidencias.

Mantener las apps abiertas. Si no conecta: revisar IP, código, misma red y aislamiento de clientes. Puede usarse un punto de acceso que permita comunicación entre clientes; no se garantiza su funcionamiento sin comprobarlo.

## EndeavourOS y Android Studio

Android Studio y SDK ya existen en `/home/seguel/Documents/Codex/herramientas/`. Abrir la carpeta del proyecto que contiene settings.gradle.kts. En Gradle JDK seleccionar JDK 21; SDK 36. No reinstalar herramientas si ya están disponibles. Para compilar: `chmod +x gradlew` y `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. La APK queda en app/build/outputs/apk/debug/app-debug.apk. local.properties es local y no debe subirse a GitHub.

Con poca batería, usar dos teléfonos existentes evita ejecutar dos emuladores. Si solo hay emuladores preparados: Base con puerto 8988, `adb -s emulator-5554 forward tcp:8988 tcp:8988`; Agente usa 10.0.2.2. Confirmar el identificador con `adb devices`.

## Guion de tres minutos

“Continuamos el control de acceso Arduino de Unidad 1. Resolvemos accesos no autorizados sin auditoría. En esta etapa dos Android simulan sensor y cerradura. Kotlin maneja la UI; SQLite conserva cuenta y eventos sin internet. Base valida las condiciones y Agente solicita. Elegimos Wi-Fi por conectividad local bidireccional sin broker. La contraseña se deriva con PBKDF2 y el código de vinculación se protege con Keystore. Los mensajes llevan AES-GCM y secuencia para detectar manipulación y repetición.”

Mostrar login, conexión, rechazo sin presencia, apertura, cierre y bitácora. Explicar que los sectores corresponden a una puerta simulada. Mostrar resultados automatizados y diferenciar los ensayos previos de las pruebas actuales. Solicitar observaciones del docente y registrar fecha, comentario, cambio y nueva verificación; no marcar validación sin recibirla.

## Preguntas frecuentes

¿Por qué SQLite? Funciona localmente sin dependencia de internet; no sincroniza cuentas entre equipos.

¿Es TLS? No: es un canal académico cifrado y autenticado con AES-GCM sobre TCP. Para producción conviene un protocolo estándar revisado.

¿Qué ocurre al perder red? No se informa éxito sin confirmación; Base conserva su cierre local temporizado.

¿Controla Arduino? No en esta entrega: los dos teléfonos simulan los dispositivos que permite la guía.

¿Cumple completamente ISO? Se aplican controles de riesgos coherentes con la orientación; no se afirma certificación.
