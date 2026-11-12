# Sumativa Unidad 2 TI3042 — MIB Acceso

**Benjamín Seguel · INACAP Temuco · 12 de noviembre de 2026**

Continuación del control de acceso Arduino de Benjamín Seguel y Marcelo Ducommun. Dos Android simulan una base con sensor y cerradura y un agente que solicita acceso por Wi-Fi. Se conserva la autoría del antecedente; esta entrega de Unidad 2 es individual.

## Entrega y documentación

El Word conserva la guía de INACAP y añade las respuestas dentro de cada apartado.

- [Guía original completada con las respuestas del proyecto](docs/Informe-Sumativa-TI3042-Benjamin-Seguel.docx)
- [Guía original de la evaluación](docs/referencias/TI3042_U2_ES_GUÍA.docx)
- [Hallazgos y pruebas de seguridad](docs/SEGURIDAD-REVISADA.md)

- [Informe técnico y correspondencia con cada criterio de la rúbrica](docs/SUMATIVA-U2.md)
- [Instalación, demo y preguntas del docente](docs/DEMO-RAPIDA.md)
- [Mockup de cinco pantallas](docs/MOCKUP.svg)
- [Resultados actuales de 18 pruebas](docs/PRUEBAS-ACTUALES.json)
- [TXT de entrega AAI](docs/Entrega_AAI_TI3042_Benjamin_Seguel.txt)
- [Guía para explicar y modificar el código](GUIA-PARA-EXPLICAR.md)

## Funciones

La aplicación Android de esta unidad está escrita en Kotlin. El archivo .ino de antecedentes es el Arduino de Unidad 1; Python y Shell se usan solo como herramientas de apoyo para pruebas o compilación.

Login local, contraseña PBKDF2 con sal, bloqueo temporal, roles Base/Agente, código de vinculación protegido con Keystore, canal AES-GCM, presencia simulada, mantenimiento, apertura temporizada y cierre autónomo, bitácora y exportación CSV.

Credenciales y eventos se conservan en SQLite. AlmacenSqlite migra los datos de la versión anterior desde SharedPreferences. No hay contraseñas universales ni secretos de misión incluidos en el repositorio.

## Abrir en Android Studio

Abrir esta carpeta raíz como proyecto. Usar Gradle JDK 21, SDK 36 y Android 8 o posterior. Ejecutar:

```bash
chmod +x gradlew
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. No versionar local.properties ni claves de firma.

Verificación realizada el 12 de noviembre: compilación correcta, 18 pruebas sin fallos y lintDebug completado. Lint reportó 20 advertencias no fatales. La firma de la APK se verificó con apksigner. Los ensayos son JUnit/Robolectric y sockets de prueba; no se afirma una nueva prueba en dos teléfonos físicos.

## Demo mínima

Instalar la misma APK en dos teléfonos conectados a la misma Wi-Fi privada. Crear cuenta local en cada uno. Iniciar Base en el primero; en Agente introducir la IP que muestra Base y su código de misión completo. Activar presencia en Base, solicitar cuatro segundos desde Agente y observar apertura y cierre. Repetir sin presencia y con mantenimiento para comprobar rechazos. Consultar y exportar la bitácora.

Mantener ambas apps abiertas. Para teléfonos usar la IPv4 de Base; 10.0.2.2 se reserva para emuladores con el reenvío indicado en la guía. Algunas redes aíslan clientes.

## Alcance y seguridad

El prototipo simula IoT: no controla Arduino real. El canal utiliza AES-GCM de aplicación sobre TCP, no TLS. Las contraseñas se derivan; el código de misión se cifra. La bitácora está en almacenamiento privado y no es un registro inmutable ni está cifrada integralmente.

La revisión y retroalimentación docente, la demostración en clase y las pruebas con Wi-Fi física siguen pendientes. No se declara certificación ISO. El [informe](docs/SUMATIVA-U2.md) relaciona riesgos, controles y límites con la orientación de [ISO/IEC 27400:2022](https://www.iso.org/standard/44373.html) y las [recomendaciones criptográficas de Android](https://developer.android.com/privacy-and-security/cryptography).
