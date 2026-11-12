# Desarrollo de la Sumativa Unidad 2

Benjamín Seguel · TI3042 · INACAP Temuco

El documento principal es [la guía completada](Informe-Sumativa-TI3042-Benjamin-Seguel.docx). Conserva la portada, las instrucciones y el diseño original. Las respuestas se añadieron debajo de cada apartado. El código completo está en este repositorio.

## Problema

Se continúa el control de acceso de la Unidad 1: impedir entradas no autorizadas y dejar un registro. En esta entrega dos Android simulan el sensor y la puerta. La Base valida y el Agente solicita. El antecedente Arduino se realizó con Marcelo Ducommun; la entrega actual es individual.

## Herramientas

Android Studio, Kotlin, SQLite y controles Material. La conexión usa Socket y ServerSocket. Para la seguridad se usan funciones de Java y Android: PBKDF2, Cipher y Keystore. La app no tiene un backend en nube.

## Comunicación

Se eligió Wi-Fi para conectar dos equipos en una misma red. La distancia depende del router y las paredes, por lo que debe comprobarse en aula. Wi-Fi puede consumir más que Bluetooth de bajo consumo. TCP mantiene el orden de los datos, pero la app también necesita respuestas y tiempos de espera.

Bluetooth sería otra alternativa con emparejamiento y permisos. MQTT funciona sobre una red y requiere un broker. Para esta práctica se mantuvo la conexión directa por TCP.

## Seguridad

El login usa una cuenta local y PBKDF2 con sal. Tres fallos producen un bloqueo de 60 segundos. El código de misión se guarda cifrado con una clave de Android Keystore. Los mensajes se cifran con AES-GCM y se revisan la sesión y la secuencia.

La Base no acepta aperturas sin presencia, con mantenimiento, con valores fuera de rango o repitiendo una orden ya procesada. Solo se declaran INTERNET y ACCESS_NETWORK_STATE. No se escanean redes ni se usa Bluetooth.

La revisión encontró dos puntos que se corrigieron: los prefijos de fórmula al exportar CSV y la comprobación de errores al escribir en SQLite. El detalle está en [SEGURIDAD-REVISADA.md](SEGURIDAD-REVISADA.md).

La app es de laboratorio. La bitácora no es inmutable ni se cifra completa. Quien consiga el código puede conectarse. El canal no es TLS y no se afirma una certificación ISO. La guía completada explica estas limitaciones.

## Pruebas

La compilación terminó correctamente y pasaron 18 pruebas con JUnit y Robolectric. Se revisaron login, bloqueo, mensajes alterados, repetición, código incorrecto, tamaño de tramas, almacenamiento y reglas de apertura. Lint dejó 20 advertencias no fatales.

Las pruebas de comunicación usan sockets dentro del computador. No se registró una prueba actual con dos radios Wi-Fi. La demostración y validación docente están pendientes. Ver [los resultados](PRUEBAS-ACTUALES.json) y los XML en [evidencias](evidencias).

## Relación con la rúbrica

| Criterio | Dónde se desarrolla |
| --- | --- |
| 2.1.1.1 y 2.1.1.2 | Paso 1 de la guía: problema, equipos y funciones. |
| 2.1.1.3 | Paso 1: herramientas y archivos del proyecto. |
| 2.1.1.4 | Paso 1: propuesta para el docente; validación pendiente. |
| 2.1.2.5 | Paso 2: selección de Wi-Fi y comparación. |
| 2.1.2.6 | Paso 2: pantallas, navegación y mockup. |
| 2.1.2.7 | Paso 2: revisión de UI y login pendiente. |
| 2.1.3.8 | Proyecto Kotlin y APK compilada. |
| 2.1.3.9 | Paso 3 y AndroidManifest.xml. |
| 2.1.3.10 | Paso 3: medidas de seguridad y límites. |
| 2.1.3.11 | Paso 3: pruebas y correcciones comprobadas. |
| 2.1.4.12 | Paso 4: secuencia de demostración preparada. |
| 2.1.4.13 | Explicaciones y ejemplos de código de la guía. |
| 2.1.4.14 | Paso 3, pruebas y revisión de seguridad. |
| 2.1.4.15 | Paso 4: preguntas y ajustes pendientes del docente. |

La tabla ubica el contenido; no asigna puntos ni reemplaza la demostración.
