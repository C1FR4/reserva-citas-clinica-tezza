# Copia de trabajo: registro de pacientes y correo

El original está en `C:\Users\USUARIO\Desktop\reserva-citas-clinica-tezza`.
Esta copia está en `C:\Users\USUARIO\Desktop\reserva-citas-clinica-tezza-registro-recordatorios`.
El código de esta ampliación se publica en la rama `C1FR4` mediante un commit posterior a los dos de la sustentación. Los secretos y datos locales permanecen fuera del repositorio.

## Entorno local utilizado para las pruebas

La copia usa su propia instancia MySQL en `127.0.0.1:3308`, base `clinica_tezza_recordatorios`, y Spring en `http://127.0.0.1:8081/InicioSesion.html`.
El registro está en `/RegistroPaciente.html`, enlazado desde el login. Las cuentas demo mantienen los accesos del proyecto original; los datos de esta base son independientes.

El buzón local está en `http://127.0.0.1:8025/`. Captura solo los mensajes enviados por SMTP a `127.0.0.1:1025`. **La aplicación se cambió posteriormente a Gmail real**: sus nuevos mensajes ya no aparecen en ese buzón local.
Los archivos EML, datos y logs de pruebas están bajo `_trabajo_apf3/`, ignorado por Git.
El ejecutable MySQL utilizado en este equipo se lee de la instalación portátil ya disponible en el original; sus datos y configuración se guardan exclusivamente en esta copia. Para trasladar el proyecto se configura MySQL propio, no se envían esos programas.

## Funciones nuevas

- Registro público con nombre, apellido, DNI, nacimiento, género, teléfono, correo, clave y confirmación.
- Siempre asigna `PACIENTE` en backend. No admite campo de rol; se rechazan campos JSON desconocidos. La creación de administrador y médico sigue siendo interna.
- Se aplican las cinco condiciones de contraseña, sin espacios, límite de 72 bytes para BCrypt y hash antes de guardar.
- Correo normalizado, usuario y DNI únicos. Cuenta y perfil se guardan en una transacción; un DNI duplicado no deja una cuenta huérfana.
- Cola persistente en `recordatorio`, conservando las siete tablas. La cita y su programación se guardan en la misma transacción.
- Ficha inmediata tras la reserva (en el siguiente ciclo del procesador).
- Recordatorio programado 24 horas antes, hora de Lima. En producción el procesador revisa cada 30 segundos por defecto; no es un reloj de precisión al segundo.
- Si se reserva con menos de 24 horas de anticipación, se envía la ficha y se evita otro correo inmediato duplicado.
- Modificación: aviso del nuevo horario y sustitución de recordatorios pendientes. Cancelación: aviso y retiro del recordatorio anterior.
- No se envían recordatorios de citas ya pasadas ni de versiones antiguas.
- Hasta cinco intentos SMTP en total, con espera de 1, 2, 4 y 8 minutos antes de los reintentos; después del quinto queda `FALLIDO` para revisión. El envío no cambia la validez de una reserva.
- Bloqueos de base de datos impiden que dos procesos envíen simultáneamente el mismo trabajo. SMTP no ofrece una transacción conjunta con MySQL: si el proceso cae después de que el servidor SMTP acepte el mensaje y antes de registrar el éxito, un reintento podría duplicarlo. No se promete entrega exactamente una vez ante esa caída.
- La entrega real y la clasificación como spam dependen también de Gmail y del destinatario.

La confirmación de asistencia por enlace y la liberación automática por falta de confirmación **no se implementan en este cambio**. Los correos remiten al paciente a Mis citas y no cancelan reservas por falta de respuesta. El registro no verifica todavía la propiedad del correo mediante un enlace de validación.

## Esquema en otra máquina

Configura una base independiente, ejecuta `database/schema.sql`, la migración `001_medicos_sin_cuenta.sql` y después `003_registro_recordatorios.sql` una sola vez.
Los `USE clinica_tezza` del DDL base y la migración 001 deben apuntar a la base que hayas elegido. En este equipo se ejecutaron contra `clinica_tezza_recordatorios`; no ejecutes esos scripts sobre el original.
La migración 003 añade versión a `cita` y tipo, programación, contador y unicidad a `recordatorio`. No crea una octava tabla ni se aplicó a la base original.
El DDL base conserva el diseño anterior del informe; esta ampliación debe reflejarse posteriormente en los diagramas/documentación si decides entregarla. El Word no fue modificado.

Para instalar desde GitHub seguir el [README](README.md): copiar `.env.example` a `.env.grupo.local` y completar los datos propios. No se incluye la configuración del Gmail utilizado durante las pruebas, ni archivos cifrados o contraseñas del equipo original. Las herramientas de `scripts/grupo/` crean cuentas internas nuevas con claves elegidas localmente.

## Gmail

La cuenta de pruebas proporcionada por el titular se autenticó correctamente en Gmail el 27/10/2026. Se verificó el envío mediante la aplicación a esa misma cuenta. El registro detallado está en `_trabajo_apf3/pruebas/RESULTADO_GMAIL_REAL.json`; la comprobación visual de bandeja corresponde al titular.
Usa una cuenta remitente con verificación en dos pasos y, si Google ofrece esa opción, una contraseña de aplicación. No uses ni compartas la contraseña habitual de Gmail.

Mantén MySQL activo y ejecuta:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
.\scripts\iniciar-gmail.ps1
```

En este equipo, el remitente se guarda en `_trabajo_apf3/gmail_usuario.local` y la contraseña de aplicación cifrada con protección de Windows (DPAPI) en `_trabajo_apf3/gmail_password.local`. Ambos archivos están ignorados por Git; el cifrado está vinculado al usuario de Windows y no es portable a otro equipo.
El script usa esa configuración local si existe; en otro equipo solicita los datos sin mostrarlos. Reinicia únicamente el proceso gestionado de esta copia en 8081 y valida la conexión SMTP al arrancar. No modifica el proyecto original.
Variables admitidas: `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_AUTH`, `MAIL_STARTTLS`, `MAIL_INTERVAL_MS`.
Gmail: host `smtp.gmail.com`, puerto `587`, autenticación y STARTTLS habilitados.
Para pruebas locales: host `127.0.0.1`, puerto `1025`, sin autenticación ni STARTTLS, remitente `clinica@tezza.test`.
`MAIL_ENABLED=false` deshabilita el procesador; las reservas siguen guardando trabajos pendientes. Al habilitarlo se enviarán los trabajos válidos pendientes al proveedor configurado. Revisa el destino antes de pasar un entorno de pruebas a Gmail.

El servidor debe permanecer encendido. Al reiniciar se recuperan trabajos vencidos que todavía correspondan a citas futuras; no se envían recordatorios de consultas pasadas.

Fuentes:
- [Spring Boot Mail](https://docs.spring.io/spring-boot/reference/io/email.html).
- [Programación de tareas de Spring](https://docs.spring.io/spring-framework/reference/integration/scheduling.html).
- [Contraseñas de aplicación de Google](https://support.google.com/accounts/answer/185833?hl=es).

## Verificaciones

Ejecuta `./mvnw.cmd test` con JDK 21 o superior. Las pruebas incluyen los casos anteriores y las nuevas de registro y cola de correo.
Los resultados de MySQL y SMTP local se guardan en `_trabajo_apf3/pruebas/RESULTADOS_MYSQL_SMTP.txt`.
Para demostrar el vencimiento real de 24 horas sin esperar un día se usa reloj fijo en tests. En la prueba SMTP se adelanta únicamente el vencimiento de un registro sintético; no se altera el reloj del equipo ni se afirma haber esperado 24 horas.
