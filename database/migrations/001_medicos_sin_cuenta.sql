-- Permite perfiles de especialistas sin crear nuevas cuentas de acceso.
-- Aplicar una sola vez después de database/schema.sql.
USE clinica_tezza;
ALTER TABLE medico MODIFY COLUMN id_usuario INT UNSIGNED NULL;
