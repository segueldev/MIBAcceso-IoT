# MIB Acceso

Trabajo individual de Benjamín Seguel para la Sumativa Unidad 2 de TI3042, INACAP Temuco.

La idea es continuar el control de acceso de la Unidad 1. El problema es que alguien puede entrar a un lugar restringido sin dejar un registro. Para esta entrega se usan dos Android: uno hace de Base y simula el sensor y la puerta; el otro hace de Agente y pide acceso por Wi-Fi.

## Archivos de la entrega

- [Guía completada](docs/Informe-Sumativa-TI3042-Benjamin-Seguel.docx): es la guía de INACAP con las respuestas añadidas en cada apartado.
- [Guía original](docs/referencias/TI3042_U2_ES_GUÍA.docx).
- [TXT para AAI](docs/Entrega_AAI_TI3042_Benjamin_Seguel.txt).
- [Pasos para probar la aplicación](docs/DEMO-RAPIDA.md).
- [Resultados de las pruebas](docs/PRUEBAS-ACTUALES.json).
- [Revisión de seguridad](docs/SEGURIDAD-REVISADA.md).
- [Explicación corta del código](GUIA-PARA-EXPLICAR.md).

## Qué hace

Tiene login, conexión entre Base y Agente, simulación de presencia, apertura por tiempo, mantenimiento y bitácora. La Base rechaza la apertura si no hay presencia o está en mantenimiento. La puerta se cierra sola al terminar el tiempo.

La aplicación usa Kotlin, SQLite y funciones de red y seguridad de Java y Android. La cuenta es local en cada celular. No usa Firebase ni MySQL.

## Abrir el proyecto

Abrir esta carpeta en Android Studio. Usar Gradle JDK 21 y SDK 36. Se necesita Android 8 o superior.

```bash
chmod +x gradlew
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

La APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Prueba con dos celulares

Conectar ambos a la misma Wi-Fi. Crear una cuenta en cada uno. Iniciar la Base y copiar su IP y código de misión en el Agente. Activar presencia en Base, pedir acceso desde Agente y observar el cierre después de cuatro segundos. Mantener las dos apps abiertas.

## Seguridad y pruebas

La contraseña se guarda mediante PBKDF2 con sal. El código de misión se protege con Android Keystore y los mensajes usan AES-GCM. También se revisan los intentos fallidos y los mensajes repetidos. Los límites y las correcciones están explicados en la guía completada.

La APK compiló y pasaron 18 pruebas automatizadas. Lint dejó 20 advertencias no fatales. Las pruebas usan JUnit, Robolectric y sockets dentro del computador. La demostración con dos Android físicos y la validación docente están pendientes. Es una simulación, sin Arduino ni cerradura real conectados.

## Otros archivos

La app Android está escrita en Kotlin. El `.ino` de `antecedentes` es el Arduino de Unidad 1, realizado con Marcelo Ducommun. Python y Shell se usan en herramientas auxiliares para pruebas o compilación; no forman parte del código Android.
