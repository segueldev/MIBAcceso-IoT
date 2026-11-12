# Revisión de seguridad del prototipo

12 de noviembre de 2026. Alcance: código del proyecto propio, pruebas locales JUnit/Robolectric y sockets TCP loopback. No es una auditoría de una instalación productiva ni de redes ajenas.

## Hallazgos corregidos

1. **Exportación CSV:** escapar comillas no evita que un campo sea interpretado como fórmula. Un par con el código de misión podía aportar un ID con prefijos =, +, - o @. La exportación antepone un apóstrofo a campos con esos prefijos o controles iniciales y conserva el escape de comillas. La prueba verifica la salida del texto. No se ejecutó en Excel: el guardado y reapertura puede modificar la protección, por lo que no se garantiza compatibilidad universal. Referencia: [OWASP CSV Injection](https://community.owasp.org/attacks/CSV_Injection).
2. **Persistencia SQLite:** no se comprobaba el retorno -1 de insertWithOnConflict. Se añadió check antes de completar transacciones para no informar persistencia exitosa cuando falla la escritura o borrar el origen tras una migración fallida. Se comprobó migración y reapertura normales; no se simuló disco lleno.

## Pruebas adversariales actuales

- Clave equivocada: el saludo no establece canal.
- Alteración de un byte: GCM rechaza la trama.
- Repetición: se rechaza la secuencia repetida.
- Sesión ajena aun con clave correcta: se rechaza.
- Trama negativa, enorme o truncada: se rechaza.
- Entrada SQL en una clave: se trata como valor literal mediante parámetros.
- Prefijos de fórmula y comillas: salida CSV neutralizada según el criterio del código.
- Acceso sin presencia, mantenimiento, duración inválida e ID duplicado: reglas verificadas.

Resultado global: 18 pruebas, cero fallos y cero errores. Compilación correcta y 20 advertencias no fatales de lint. Resultados en PRUEBAS-ACTUALES.json y XML en evidencias/; se retiraron de las copias de XML los atributos de host y salidas de diagnóstico, conservando conteos, fechas y casos.

## Límites y riesgos residuales

La APK es debug; no hay cuentas centrales ni identidad individual del par más allá de poseer el secreto. El bloqueo depende del reloj del teléfono. La bitácora no es inmutable ni cifrada integralmente. Android Keystore no implica un respaldo físico verificado. Una fuente insistente puede afectar disponibilidad porque Base atiende un cliente; no se hizo ensayo DoS. El canal es AES-GCM sobre TCP, no TLS ni un protocolo certificado, y carece de secreto futuro. El equipo comprometido puede superar controles. La app requiere permanecer abierta y simula una puerta: no existe actuador físico conectado.

Estos controles se relacionan con riesgos IoT de ISO/IEC 27400; no se declara certificación ni cumplimiento integral. La prueba física, demostración y validación docente permanecen pendientes.
