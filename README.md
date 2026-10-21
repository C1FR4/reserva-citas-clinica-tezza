# Sistema de reserva de citas médicas — Clínica Padre Luis Tezza

## Estado de la rama C1FR4

Esta rama integra el proyecto Spring Boot iniciado por una compañera con las pantallas HTML/CSS existentes. Ya funciona el acceso de pacientes con contraseña BCrypt y sesión HTTP, además de la consulta, reserva y cancelación de citas vinculadas a MySQL. El panel y la página de especialistas muestran datos reales. También se conserva el panel administrativo inicial para registrar médicos, ajustado a los campos obligatorios del esquema.

## Estructura del proyecto

La base Maven se generó con Spring Initializr para Java 21 y Spring Boot 4.1.1. El código de la aplicación está en `src/main/java/com/clinica/clinica`, las pantallas y sus recursos en `src/main/resources/static`, la vista administrativa Thymeleaf en `src/main/resources/templates`, y las pruebas en `src/test/java`. Las antiguas maquetas con nombres y médicos ficticios se conservan en `legacy/`, fuera del sitio servido por Spring.

Las cinco dependencias principales son Spring Web MVC, Spring Data JPA, Thymeleaf, Validation y MySQL Driver. `spring-security-crypto` permite verificar contraseñas BCrypt sin activar una segunda pantalla de login. H2 y los módulos de pruebas de Spring solo se usan al ejecutar tests. Data JPA ya incluye JDBC y el pool de conexiones, por lo que no hace falta agregar otro conector MySQL.

El archivo `database/schema.sql` es una copia **idéntica** del DDL validado en el informe APF3. El archivo `script-prueba-incompleto.sql` recibido con el avance de la compañera no se ejecuta: define solo dos tablas, usa nombres/rol distintos y guarda una contraseña sin cifrar.

Para el avance de reserva se agregó `database/migrations/001_medicos_sin_cuenta.sql`. Esta migración permite que un perfil médico de demostración tenga `id_usuario` nulo, sin crear nuevas cuentas de acceso. Es el único cambio respecto al DDL del informe: las siete tablas, claves y relaciones permanecen. Tras aplicarla, `database/demo-especialistas.sql` agrega tres especialistas ficticios y sus turnos de forma idempotente. Las cuentas de prueba siguen siendo solo `usuariotest` y `admin`.

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

Después de ejecutar el DDL, puedes iniciar una vez el perfil `seed`. Inserta `usuariotest` como paciente y `admin` como administrador, además de un médico con horarios de lunes a viernes de 08:00 a 13:00 si aún no existen. El médico de demostración comparte la fila `admin` en `usuario` para conservar la clave foránea obligatoria del DDL sin crear una tercera cuenta de prueba. Ambas contraseñas se cifran con BCrypt. Usa un DNI de prueba de ocho dígitos que no esté repetido.

```powershell
$env:SEED_EMAIL = 'usuariotest'
$env:SEED_PASSWORD = Read-Host 'Clave de prueba (mínimo 8 caracteres)' -MaskInput
$env:SEED_ADMIN_PASSWORD = Read-Host 'Clave del administrador (mínimo 8 caracteres)' -MaskInput
$env:SEED_NOMBRE = 'Usuario'
$env:SEED_APELLIDO = 'Test'
$env:SEED_DNI = '12345678'
$env:SEED_FECHA_NACIMIENTO = '1995-05-20'
$env:SEED_GENERO = 'FEMENINO'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=seed'
```

Cuando termine el perfil `seed`, detén el servidor y vuelve a iniciarlo sin ese perfil. Elimina las variables temporales con `Remove-Item Env:SEED_PASSWORD, Env:SEED_ADMIN_PASSWORD`.

Para mostrar cuatro especialistas sin crear más cuentas, ejecuta después estos archivos en MySQL Workbench o con el cliente `mysql`, en este orden:

```text
database/migrations/001_medicos_sin_cuenta.sql
database/demo-especialistas.sql
```

El segundo script se puede volver a ejecutar: identifica los médicos por correo y evita repetir los turnos. La pantalla **Especialistas** muestra los días y horas de cada uno; **Reservar cita** propone la próxima fecha en que atiende el médico elegido y permite cambiarla.

## Comprobación del flujo

1. Abre `http://localhost:8080/InicioSesion.html` e inicia sesión con la cuenta de prueba.
2. Verifica que `PantallaPrincipal.html` muestre el nombre registrado.
3. Cierra sesión y prueba abrir directamente `http://localhost:8080/PantallaPrincipal.html`: debe volver al login.
4. Una clave incorrecta debe mostrar un error y no abrir el panel.
5. En el panel, abre **Reservar cita**, elige una fecha laboral y uno de los espacios disponibles. La reserva debe aparecer en **Mis citas** y en el panel. Desde **Mis citas** puedes cancelar una reserva futura.
6. En **Especialistas**, comprueba que el listado provenga de MySQL. El botón del médico debe abrir **Reservar cita** con ese especialista seleccionado. Desde la reserva, vuelve a **Especialistas** y comprueba que el enlace de **Mis citas** siga funcionando y que el nombre del paciente sea correcto.

Pruebas automáticas del controlador:

```powershell
.\mvnw.cmd test
```

## Alcance pendiente

- La reserva usa espacios de 30 minutos dentro de los rangos de `horario`. El DDL impide que dos citas activas ocupen el mismo espacio del médico o del paciente.
- El alta de médicos heredada de la compañera se ha ajustado al DDL y al cifrado, pero falta comprobarla frente a una instancia MySQL real.
- La integración con MySQL 8.4.11 se verificó localmente el 21/10/2026 con el DDL de siete tablas: arranque con `ddl-auto=validate`, creación de un paciente de prueba, login, lectura del panel y cierre de sesión. En otra máquina se debe configurar su propia instancia MySQL y las variables `DB_*`.
