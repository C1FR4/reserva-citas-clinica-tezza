# Reserva de citas médicas — Clínica Tezza

Aplicación Java/Spring Boot con MySQL y tres roles: paciente, administrador y médico. La rama `C1FR4` incorpora registro público exclusivo de pacientes y notificaciones por correo, conservando el historial de login, permisos y CRUD.

## Funciones

- Registro de pacientes con correo, datos personales y contraseña BCrypt. El servidor fija el rol `PACIENTE`; no admite elegir roles internos en el registro público.
- Login con sesión HTTP, controles de acceso por rol y consultas limitadas al propietario de los registros.
- Consulta de especialistas y cupos; reserva, modificación hasta dos veces y cancelación con al menos 24 horas de anticipación.
- Administración de médicos, horarios y citas; exportación XLSX y eliminación física de citas previamente canceladas.
- Agenda médica propia y registro manual de `ASISTIO` o `NO_ASISTIO` cuando comienza una cita activa, con asistencia pendiente.
- Ficha por correo y recordatorio programado 24 horas antes, con cola persistente, reintentos y reprogramación al modificar o cancelar.
- Herramienta local para crear cuentas internas de administrador y médico. No existe un registro web público para esos roles.

La confirmación por enlace, la liberación automática por falta de respuesta y la historia clínica no forman parte de este avance. La exportación XLSX es un listado; no representa todos los indicadores agregados del informe.

## Requisitos

- JDK 21 o superior.
- MySQL 8 con soporte de restricciones `CHECK`; recomendado MySQL 8.4.
- Internet para descargar las dependencias iniciales mediante Maven Wrapper.
- Un servicio SMTP configurado si se desean correos reales.

Node.js es opcional y se utiliza para comprobar sintaxis JavaScript. El servidor se ejecuta con Spring Boot.

## Instalación nueva de la base

Desde MySQL Workbench o el cliente `mysql`, ejecutar en este orden, una sola vez sobre una base nueva:

1. `database/schema.sql`: crea `clinica_tezza` y sus siete tablas.
2. `database/migrations/001_medicos_sin_cuenta.sql`: permite perfiles médicos sin cuenta de acceso.
3. `database/migrations/003_registro_recordatorios.sql`: añade la programación y control de notificaciones.
4. Opcionalmente, `database/demo-especialistas.sql`: agrega especialistas ficticios y sus horarios, sin credenciales de acceso.

Si se usa otro nombre de base, ajustar las instrucciones `CREATE DATABASE`/`USE` de los scripts y `DB_NAME` en la configuración local. Para bases antiguas con el vínculo provisional del administrador al médico existe `002_desvincular_admin_medico.sql`; no es necesaria en una instalación nueva.

`spring.jpa.hibernate.ddl-auto=validate` comprueba los mapeos al iniciar; no sustituye estos scripts. No se publican volcados de la base usada durante las pruebas ni credenciales de cuentas reales.

## Configuración local

En PowerShell, desde la raíz del proyecto:

```powershell
Copy-Item .env.example .env.grupo.local
```

Editar `.env.grupo.local` con el host, puerto, base y usuario de MySQL propios. Si `DB_PASSWORD` queda vacío, las herramientas la solicitan localmente sin mostrarla. El ejemplo usa MySQL en `3306`, base `clinica_tezza` y aplicación en `8081`.

Este archivo local está ignorado por Git. No se proporcionan credenciales SMTP: completar `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM` localmente, y activar `MAIL_ENABLED=true` para enviar correos. Con `MAIL_ENABLED=false` el procesador permanece deshabilitado.

Los scripts de `scripts/grupo/` cargan ese archivo. Spring no carga un `.env` automáticamente; al ejecutar desde un IDE o directamente con Maven se deben definir las variables equivalentes.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/grupo/iniciar.ps1
```

Abrir `http://127.0.0.1:8081/InicioSesion.html`. Conservar la terminal abierta; `Ctrl+C` detiene el servidor. No abrir los HTML mediante `file://` o Live Server.

## Crear cuentas

Los pacientes se registran desde **Regístrate aquí** en el login y luego acceden con su correo. El registro no solicita la contraseña del buzón del paciente.

Para crear un administrador propio:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/grupo/crear-admin.ps1
```

La herramienta pide usuario, datos de contacto y contraseña. Se ejecuta con el perfil interno `cuenta-interna`, sin servidor web, y no sobrescribe cuentas existentes.

Para crear el acceso de un médico, primero debe existir su perfil activo y sin cuenta. Se puede usar un especialista de demostración o registrarlo desde el panel administrativo. Después:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/grupo/crear-medico.ps1
```

Indicar el DNI del perfil, un usuario nuevo y la contraseña elegida. El vínculo no cambia el correo, los datos ni los horarios del especialista. Crear un perfil desde el panel no genera automáticamente su cuenta.

Todas las herramientas nuevas aplican cinco condiciones: mínimo ocho caracteres, minúscula, mayúscula, dígito y símbolo; además rechazan espacios y claves que excedan 72 bytes para BCrypt. Los inicializadores de demostración anteriores siguen disponibles bajo sus perfiles explícitos, pero no se ejecutan al arrancar normalmente.

## Correo

Spring Mail utiliza la configuración SMTP local. Gmail admite `smtp.gmail.com:587` con autenticación y STARTTLS; requiere credenciales compatibles con la cuenta remitente. Ninguna contraseña o remitente personal se incluye en este repositorio.

La cola reside en la tabla `recordatorio`, dentro de las siete tablas originales. Los mensajes se guardan en la misma transacción que la reserva y el procesador los envía en ciclos de 30 segundos por defecto. El servidor debe permanecer encendido; las citas ya pasadas no reciben recordatorios atrasados. Reservas con menos de 24 horas reciben la ficha sin un segundo aviso inmediato.

Para detalles de reintentos, cancelaciones, prueba SMTP local y límites de entrega, consultar [REGISTRO_Y_RECORDATORIOS.md](REGISTRO_Y_RECORDATORIOS.md). El script auxiliar `iniciar-gmail.ps1` conserva compatibilidad con la configuración privada de este equipo; para otra instalación usar las variables propias o las herramientas de `scripts/grupo/`.

## Estructura

- `src/main/java/com/clinica/clinica`: modelos, repositorios, servicios, controladores y configuración.
- `src/main/resources/static`: portal del paciente y recursos frontend.
- `src/main/resources/templates`: vistas Thymeleaf del administrador y médico.
- `database`: DDL, migraciones y catálogo médico ficticio.
- `src/test/java`: pruebas de permisos, contraseñas, registro, CRUD, asistencia, reportes y correo.

## Verificación

```powershell
.\mvnw.cmd test
node --run check:js
```

La versión ampliada se comprobó con 39 pruebas automatizadas, flujos en MySQL, SMTP local, registro en navegador y envío real de prueba autorizado. Los tests usan simulaciones y H2; no todos ejecutan MySQL ni envían correos. Los registros y capturas locales se conservan fuera de Git.

## Evidencias para la sustentación

Se conservan los commits `3a77eae` (login, roles y contraseñas) y `253eb91` (CRUD de citas). Las ampliaciones se publican en un commit posterior, sin reescribir esos antecedentes. Para exponer las evidencias históricas se deben abrir sus archivos en el commit correspondiente.
