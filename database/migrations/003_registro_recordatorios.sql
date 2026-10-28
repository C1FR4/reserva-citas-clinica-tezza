-- Ejecutar una sola vez, en la base de la COPIA. Mantiene las siete tablas.
ALTER TABLE cita ADD COLUMN version_notificacion INT NOT NULL DEFAULT 0;
ALTER TABLE recordatorio
  ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'RECORDATORIO',
  ADD COLUMN version_cita INT NOT NULL DEFAULT 0,
  ADD COLUMN programado_para DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN proximo_intento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN intentos INT NOT NULL DEFAULT 0,
  ADD CONSTRAINT uq_recordatorio_version UNIQUE (id_cita, version_cita, tipo),
  ADD CONSTRAINT ck_recordatorio_tipo CHECK (tipo IN ('FICHA','CAMBIO','CANCELACION','RECORDATORIO'));
CREATE INDEX idx_recordatorio_pendiente ON recordatorio (estado, proximo_intento);
