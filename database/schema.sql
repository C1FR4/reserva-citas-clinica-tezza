CREATE DATABASE IF NOT EXISTS clinica_tezza CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clinica_tezza;

CREATE TABLE usuario (
  id_usuario INT UNSIGNED AUTO_INCREMENT,
  usuario VARCHAR(120) NOT NULL,
  contrasena VARCHAR(255) NOT NULL,
  rol VARCHAR(20) NOT NULL,
  CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
  CONSTRAINT uq_usuario_usuario UNIQUE (usuario),
  CONSTRAINT ck_usuario_rol CHECK (rol IN ('PACIENTE','MEDICO','ADMINISTRADOR'))
) ENGINE=InnoDB;

CREATE TABLE administrador (
  id_administrador INT UNSIGNED AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  apellido VARCHAR(100) NOT NULL,
  correo VARCHAR(120) NOT NULL,
  id_usuario INT UNSIGNED NOT NULL,
  CONSTRAINT pk_administrador PRIMARY KEY (id_administrador),
  CONSTRAINT uq_administrador_correo UNIQUE (correo),
  CONSTRAINT uq_administrador_usuario UNIQUE (id_usuario),
  CONSTRAINT fk_administrador_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

CREATE TABLE paciente (
  id_paciente INT UNSIGNED AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  apellido VARCHAR(100) NOT NULL,
  genero VARCHAR(20) NOT NULL,
  fecha_nacimiento DATE NOT NULL,
  dni CHAR(8) NOT NULL,
  telefono VARCHAR(20) NULL,
  id_usuario INT UNSIGNED NOT NULL,
  CONSTRAINT pk_paciente PRIMARY KEY (id_paciente),
  CONSTRAINT uq_paciente_dni UNIQUE (dni),
  CONSTRAINT uq_paciente_usuario UNIQUE (id_usuario),
  CONSTRAINT fk_paciente_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

CREATE TABLE medico (
  id_medico INT UNSIGNED AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  apellido VARCHAR(100) NOT NULL,
  especialidad VARCHAR(100) NOT NULL,
  anios_experiencia SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  areas_especializacion VARCHAR(255) NULL,
  correo VARCHAR(120) NOT NULL,
  telefono VARCHAR(20) NULL,
  dni CHAR(8) NOT NULL,
  estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
  id_usuario INT UNSIGNED NOT NULL,
  CONSTRAINT pk_medico PRIMARY KEY (id_medico),
  CONSTRAINT uq_medico_dni UNIQUE (dni),
  CONSTRAINT uq_medico_correo UNIQUE (correo),
  CONSTRAINT uq_medico_usuario UNIQUE (id_usuario),
  CONSTRAINT ck_medico_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
  CONSTRAINT fk_medico_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

CREATE TABLE horario (
  id_horario INT UNSIGNED AUTO_INCREMENT,
  id_medico INT UNSIGNED NOT NULL,
  dia VARCHAR(15) NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fin TIME NOT NULL,
  CONSTRAINT pk_horario PRIMARY KEY (id_horario),
  CONSTRAINT uq_horario_franja UNIQUE (id_medico, dia, hora_inicio),
  CONSTRAINT uq_horario_id_medico UNIQUE (id_horario, id_medico),
  CONSTRAINT ck_horario_dia CHECK (dia IN ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO')),
  CONSTRAINT ck_horario_horas CHECK (hora_fin > hora_inicio),
  CONSTRAINT fk_horario_medico FOREIGN KEY (id_medico) REFERENCES medico (id_medico)
) ENGINE=InnoDB;

CREATE TABLE cita (
  id_cita INT UNSIGNED AUTO_INCREMENT,
  fecha DATE NOT NULL,
  hora TIME NOT NULL,
  tipo_consulta VARCHAR(12) NOT NULL,
  motivo VARCHAR(255) NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'RESERVADA',
  asistencia VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  numero_modificaciones TINYINT UNSIGNED NOT NULL DEFAULT 0,
  fecha_solicitud DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_confirmacion DATETIME NULL,
  id_paciente INT UNSIGNED NOT NULL,
  id_medico INT UNSIGNED NOT NULL,
  id_horario INT UNSIGNED NOT NULL,
  cupo_activo TINYINT UNSIGNED GENERATED ALWAYS AS (IF(estado = 'CANCELADA', NULL, 1)) STORED,
  CONSTRAINT pk_cita PRIMARY KEY (id_cita),
  CONSTRAINT uq_cita_cupo UNIQUE (id_medico, fecha, hora, cupo_activo),
  CONSTRAINT uq_cita_paciente_franja UNIQUE (id_paciente, fecha, hora, cupo_activo),
  CONSTRAINT ck_cita_tipo CHECK (tipo_consulta IN ('PRESENCIAL','VIRTUAL')),
  CONSTRAINT ck_cita_estado CHECK (estado IN ('RESERVADA','CONFIRMADA','CANCELADA')),
  CONSTRAINT ck_cita_asistencia CHECK (asistencia IN ('PENDIENTE','ASISTIO','NO_ASISTIO')),
  CONSTRAINT ck_cita_modificaciones CHECK (numero_modificaciones <= 2),
  CONSTRAINT ck_cita_confirmacion CHECK ((estado <> 'CONFIRMADA' OR fecha_confirmacion IS NOT NULL) AND (fecha_confirmacion IS NULL OR fecha_confirmacion >= fecha_solicitud)),
  CONSTRAINT fk_cita_paciente FOREIGN KEY (id_paciente) REFERENCES paciente (id_paciente),
  CONSTRAINT fk_cita_medico FOREIGN KEY (id_medico) REFERENCES medico (id_medico),
  CONSTRAINT fk_cita_horario_medico FOREIGN KEY (id_horario, id_medico) REFERENCES horario (id_horario, id_medico)
) ENGINE=InnoDB;

CREATE TABLE recordatorio (
  id_recordatorio INT UNSIGNED AUTO_INCREMENT,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  mensaje VARCHAR(255) NOT NULL,
  fecha_envio DATETIME NULL,
  id_cita INT UNSIGNED NOT NULL,
  CONSTRAINT pk_recordatorio PRIMARY KEY (id_recordatorio),
  CONSTRAINT ck_recordatorio_estado CHECK (estado IN ('PENDIENTE','ENVIADO','FALLIDO')),
  CONSTRAINT fk_recordatorio_cita FOREIGN KEY (id_cita) REFERENCES cita (id_cita) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_cita_fecha ON cita (fecha);
