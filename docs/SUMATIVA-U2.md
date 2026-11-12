# Control de acceso con auditoría mediante dos dispositivos Android

Sumativa Unidad 2 · TI3042 · Benjamín Seguel · INACAP Temuco

## Problema y continuidad

El problema es el ingreso no autorizado a áreas restringidas sin una bitácora consultable. El antecedente de Unidad 1 pertenece a Benjamín Seguel y Marcelo Ducommun: Arduino, detector de presencia, teclado, LCD y servomotor. El Word inicial describe PIR; el informe técnico posterior describe sensor ultrasónico y Bluetooth HC-05. Se conserva el objetivo común de detectar presencia, validar acceso, actuar y registrar. En esta unidad el sensor se simula como una señal booleana PIR: no se afirma medir distancias ni controlar hardware Arduino real.

La guía permite usar dos Android para simular control y monitoreo IoT. Una misma APK ofrece Base y Agente. La base simula sensor, cerradura y mantenimiento; el agente solicita acceso y recibe confirmaciones. Los sectores son destinos de una misma puerta simulada.

## Funcionalidades y herramientas

Alta de cuenta local sin contraseña universal, login, bloqueo tras tres fallos, cierre de sesión, roles Base y Agente, vinculación mediante código de misión, telemetría, apertura entre 1 y 10 segundos, cierre autónomo, rechazo sin presencia o durante mantenimiento, consulta de eventos y exportación CSV.

Android Studio y Kotlin permiten producir una APK nativa. La interfaz usa Material Components; AndroidViewModel conserva estado ante recreaciones de pantalla. Los sockets operan fuera del hilo de interfaz. SQLite funciona sin internet y evita credenciales de un servicio externo. La clase AlmacenSqlite almacena pares clave/valor en una tabla local y migra los datos anteriores de SharedPreferences: usuario, sal, verificador, bloqueo, código cifrado y un registro JSON de hasta 100 eventos. No se utiliza Firebase ni MySQL. El agente consulta el estado remoto mediante mensajes del protocolo y registra localmente lo recibido. No hay una base central compartida entre ambos equipos.

## Comunicación inalámbrica

Se selecciona TCP sobre Wi-Fi de una misma red privada porque permite comunicación bidireccional entre dos teléfonos sin un broker. El alcance depende del punto de acceso, muros e interferencia; debe validarse en aula y no se declara una distancia medida. Wi-Fi consume más que BLE, por lo que la demo mantiene sesiones cortas y las apps abiertas. Bluetooth reduce infraestructura, pero añade emparejamiento y permisos; MQTT es un protocolo de mensajería sobre una red, no una radio alternativa, y requeriría un broker. TCP conserva el orden, pero la app igualmente valida confirmaciones, secuencias, tiempos de espera y reconexión. Algunas redes de invitados impiden comunicación entre clientes.

## Diseño de interfaz y navegación

El mockup está en `MOCKUP.svg`. Login permite crear e ingresar una credencial y muestra errores o bloqueo. Tras autenticarse se navega entre Monitoreo, Conexión, Activador y Bitácora. Conexión selecciona rol y configura dirección/código; Monitoreo muestra enlace y estado de puerta; Activador muestra sensor y mantenimiento en Base y solicitud/duración en Agente. Bitácora presenta fecha, evento e identificador y permite exportar. El estado sin conexión no debe interpretarse como apertura confirmada. El mockup es un esquema de diseño, no una captura ni validación docente.

## Seguridad y relación con ISO IEC 27400

ISO/IEC 27400:2022 entrega orientación de riesgos y controles de seguridad y privacidad IoT. Este mapeo es una decisión de diseño del prototipo, no una certificación ni una auditoría de conformidad.

| Riesgo | Control implementado | Evidencia que mostrar |
| --- | --- | --- |
| Robo de contraseña almacenada | PBKDF2-HMAC-SHA256, sal aleatoria y 210.000 iteraciones; no se guarda la contraseña | Credenciales.kt y CanalSeguro.kt |
| Intentos repetidos | Bloqueo persistente de 60 segundos tras tres fallos | Login y pruebas de credenciales |
| Lectura o alteración de órdenes | AES-256-GCM con IV aleatorio y etiqueta de integridad | Pruebas de alteración y clave incorrecta |
| Repetición de mensajes | Desafío de sesión y secuencias estrictas | CanalSeguroTest |
| Exposición del secreto de vinculación | Código aleatorio de 256 bits cifrado mediante Android Keystore en Base | Credenciales.kt |
| Apertura indebida | Base valida presencia, mantenimiento, sector, duración e ID duplicado | PuertaTest |
| Pérdida de enlace | Cierre temporizado local y ausencia de reenvío automático de aperturas | Demo con desconexión |
| Filtración de auditoría | Datos en espacio privado, sin contraseñas en eventos; exportación elegida por usuario; copias excluidas | Manifest y exportarCsv |

El hash de contraseña no equivale a cifrado reversible. El código de misión sí se cifra; el canal usa cifrado autenticado de aplicación sobre TCP, no TLS. `usesCleartextTraffic=false` no protege por sí solo sockets: AES-GCM lo hace en CanalSeguro. Los permisos INTERNET y ACCESS_NETWORK_STATE permiten red y consulta de conectividad sin permisos peligrosos de ubicación o Bluetooth porque no hay escaneo de redes ni uso de Bluetooth.

Limitaciones: la cuenta es local, poseer el código habilita la vinculación y no identifica a un agente mediante servidor central; la bitácora no es inmutable ni está cifrada integralmente; un dispositivo comprometido puede alterar datos; el bloqueo depende del reloj; el canal académico requiere revisión independiente. La base debe permanecer abierta y no controla un actuador real. Se debe validar con el docente si requiere específicamente TLS como evolución.

## Verificación y criterios

Los resultados nuevos se publican en `docs/PRUEBAS-ACTUALES.json`. Las capturas y JSON incluidos en el ZIP recibido corresponden a antecedentes de una versión previa: no acreditan pruebas nuevas de esta entrega ni del cambio SQLite. No se afirma haber probado hoy en dos teléfonos reales.

| Criterios | Evidencia preparada |
| --- | --- |
| 2.1.1.1 y 2.1.1.2 | Problema, componentes y funcionalidades anteriores |
| 2.1.1.3 | Herramientas, arquitectura y SQLite |
| 2.1.1.4 | Propuesta lista para presentar; retroalimentación docente pendiente |
| 2.1.2.5 | Comparación Wi-Fi, Bluetooth y MQTT |
| 2.1.2.6 | Mockup y navegación |
| 2.1.2.7 | Diseño y autenticación listos para revisión; validación pendiente |
| 2.1.3.8 y 2.1.3.9 | Proyecto Kotlin, APK y Manifest |
| 2.1.3.10 y 2.1.4.14 | Controles y límites de seguridad |
| 2.1.3.11 | Pruebas actuales automatizadas y plan manual |
| 2.1.4.12 | Demo de dos Android pendiente de ejecución en clase |
| 2.1.4.13 | Guion y decisiones técnicas |
| 2.1.4.15 | Preguntas y ajustes con docente pendientes |

No se asigna un puntaje anticipado: corresponde al docente verificar las evidencias y la demostración.

## Fuentes

Guía Sumativa TI3042 U2 y antecedentes Arduino entregados por el estudiante.

- ISO: https://www.iso.org/standard/44373.html
- Android Developers, criptografía: https://developer.android.com/privacy-and-security/cryptography
- Android Keystore: https://developer.android.com/privacy-and-security/keystore
