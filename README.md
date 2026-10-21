# Sistema de reserva de citas médicas — Clínica Padre Luis Tezza

## Estado de la rama C1FR4

Esta rama integra el proyecto Spring Boot iniciado por una compañera con las pantallas HTML/CSS existentes. El primer flujo funcional es el acceso de pacientes: inicio de sesión con contraseña BCrypt, sesión HTTP, panel protegido, nombre real del paciente y cierre de sesión. También se conserva el panel administrativo inicial para registrar médicos, ajustado a los campos obligatorios del esquema. Las tarjetas y citas del panel del paciente siguen siendo datos de maqueta; la reserva real corresponde al siguiente avance.

El archivo `database/schema.sql` es una copia **idéntica** del DDL validado en el informe APF3. El archivo `script-prueba-incompleto.sql` recibido con el avance de la compañera no se ejecuta: define solo dos tablas, usa nombres/rol distintos y guarda una contraseña sin cifrar.

## Requisitos

- JDK 21 o superior.
- MySQL 8 con un servidor en ejecución.
- Acceso a MySQL Workbench o al cliente `mysql` para ejecutar `database/schema.sql` una vez. El script crea la base `clinica_tezza` y sus siete tablas.

En PowerShell, si Maven selecciona un Java antiguo:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
```

El proyecto incluye Maven Wrapper, así que no requiere instalar Maven aparte.

## Configuración local

`application.properties` lee `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` y `PORT`. `.env.example` muestra los nombres; Spring no carga ese archivo automáticamente. Define las variables en tu terminal antes de iniciar:

```powershell
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '3306'
$env:DB_NAME = 'clinica_tezza'
$env:DB_USER = 'clinica_app'
$env:DB_PASSWORD = Read-Host 'Contraseña de MySQL' -MaskInput
.\mvnw.cmd spring-boot:run
```

Abre `http://localhost:8080/`. No abras los HTML como archivos locales ni mediante Live Server: el login requiere el servidor Spring.

Para comprobar la conexión, el inicio de Spring valida las entidades contra el esquema (`ddl-auto=validate`). Si falta MySQL o el DDL, la aplicación informa el fallo al arrancar. No crea ni cambia tablas automáticamente.

## Crear un paciente de prueba

Después de ejecutar el DDL, puedes iniciar una vez el perfil `seed`. Este perfil solo inserta un paciente si su usuario aún no existe y cifra la contraseña con BCrypt. Usa un DNI de prueba de ocho dígitos que no esté repetido.

```powershell
$env:SEED_EMAIL = 'paciente@tezza.test'
$env:SEED_PASSWORD = Read-Host 'Clave de prueba (mínimo 8 caracteres)' -MaskInput
$env:SEED_NOMBRE = 'Ana'
$env:SEED_APELLIDO = 'Pérez'
$env:SEED_DNI = '12345678'
$env:SEED_FECHA_NACIMIENTO = '1995-05-20'
$env:SEED_GENERO = 'FEMENINO'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=seed'
```

Cuando aparezca `Paciente de prueba creado.`, detén el servidor y vuelve a iniciarlo sin el perfil `seed`. Elimina la variable temporal de la contraseña con `Remove-Item Env:SEED_PASSWORD`.

## Comprobación del flujo

1. Abre `http://localhost:8080/InicioSesion.html` e inicia sesión con la cuenta de prueba.
2. Verifica que `PantallaPrincipal.html` muestre el nombre registrado.
3. Cierra sesión y prueba abrir directamente `http://localhost:8080/PantallaPrincipal.html`: debe volver al login.
4. Una clave incorrecta debe mostrar un error y no abrir el panel.

Pruebas automáticas del controlador:

```powershell
.\mvnw.cmd test
```

## Alcance pendiente

- El formulario de reserva y las citas que aparecen en el panel todavía no usan `cita` ni `horario`.
- El alta de médicos heredada de la compañera se ha ajustado al DDL y al cifrado, pero falta comprobarla frente a una instancia MySQL real.
- La integración con MySQL 8.4.11 se verificó localmente el 21/10/2026 con el DDL de siete tablas: arranque con `ddl-auto=validate`, creación de un paciente de prueba, login, lectura del panel y cierre de sesión. En otra máquina se debe configurar su propia instancia MySQL y las variables `DB_*`.
