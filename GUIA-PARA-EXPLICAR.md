# Para entender el proyecto

## La idea

La Base representa la puerta y el sensor. El Agente pide acceso. La Base revisa las condiciones, responde y cierra la puerta cuando termina el tiempo. Son dos instalaciones de la misma aplicación.

## Dónde está cada parte

- `MainActivity.kt`: muestra las pantallas y sus botones.
- `ModeloAgencia.kt`: relaciona los botones con las acciones, guarda el estado y prepara la bitácora.
- `Credenciales.kt`: crea la cuenta, revisa la contraseña y lleva el bloqueo por intentos.
- `AlmacenSqlite.kt`: guarda y consulta los datos locales.
- `ConexionWifi.kt`: espera o inicia la conexión TCP.
- `CanalSeguro.kt`: usa las funciones de Java para proteger los mensajes y revisar su orden.
- `Puerta.kt`: decide si se abre y durante cuánto tiempo.
- `AjustesAgencia.kt`: tiene los tiempos permitidos, destinos y mensajes.

Estos archivos están en `app/src/main/java/cl/seguel/mib`.

## Seguir una solicitud

1. El botón de acceso llama a `solicitarAcceso()`.
2. El Agente envía el destino y la duración, por ejemplo `1|4`.
3. La Base recibe los datos y llama a `Puerta.abrir()`.
4. Si falta presencia, hay mantenimiento o los valores son incorrectos, rechaza la orden.
5. Si acepta, devuelve el resultado y el estado de la puerta.
6. Al terminar el tiempo, la Base cierra por su cuenta y avisa al Agente.

El identificador relaciona la orden con su respuesta. Si se pierde el enlace, el Agente no debe mostrar que abrió sin haber recibido confirmación.

## Qué explicar de la seguridad

La contraseña no se guarda como texto. PBKDF2 calcula un resultado usando una sal aleatoria. La cuenta se revisa comparando ese resultado. El código de misión es otro secreto y sirve para conectar los equipos.

AES-GCM cifra los mensajes y detecta cambios. La sesión y el número de mensaje ayudan a rechazar repeticiones. Android Keystore protege la clave con la que se guarda el código en Base.

No se creó un algoritmo criptográfico: se llaman funciones que ya existen. El protocolo de mensajes de la práctica es propio y no equivale a TLS. La bitácora no es inmutable. La aplicación requiere permanecer abierta y no controla una puerta real.

## Cambios pequeños para practicar

En `AjustesAgencia.kt` se pueden leer el tiempo inicial de 4 segundos, el mínimo de 1 y el máximo de 10. También están los nombres de los destinos. Después de cambiar un valor hay que compilar y usar la misma versión en ambos equipos.

Los resultados actuales están en `docs/PRUEBAS-ACTUALES.json`. Cambiar un valor o leer esta guía no cuenta por sí solo como una prueba realizada. La demostración y las observaciones del docente deben registrarse después de hacerlas.
