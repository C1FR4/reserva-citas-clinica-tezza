-- Datos ficticios para probar reservas. No crea usuarios ni contraseñas.
-- Requiere database/migrations/001_medicos_sin_cuenta.sql.
USE clinica_tezza;

INSERT INTO medico (nombre, apellido, especialidad, anios_experiencia, correo, dni, estado, id_usuario)
SELECT CONVERT(0x46C3A174696D61 USING utf8mb4), 'Robles Castro', CONVERT(0x4F6674616C6D6F6C6F67C3AD61 USING utf8mb4), 8, 'fatima.robles@tezza.test', '87654322', 'ACTIVO', NULL
WHERE NOT EXISTS (SELECT 1 FROM medico WHERE correo = 'fatima.robles@tezza.test');
INSERT INTO medico (nombre, apellido, especialidad, anios_experiencia, correo, dni, estado, id_usuario)
SELECT 'Luis', CONVERT(0x436F6E747265726173204CC3B370657A USING utf8mb4), CONVERT(0x43617264696F6C6F67C3AD61 USING utf8mb4), 12, 'luis.contreras@tezza.test', '87654323', 'ACTIVO', NULL
WHERE NOT EXISTS (SELECT 1 FROM medico WHERE correo = 'luis.contreras@tezza.test');
INSERT INTO medico (nombre, apellido, especialidad, anios_experiencia, correo, dni, estado, id_usuario)
SELECT 'Ana', 'Vargas Medina', CONVERT(0x50656469617472C3AD61 USING utf8mb4), 7, 'ana.vargas@tezza.test', '87654324', 'ACTIVO', NULL
WHERE NOT EXISTS (SELECT 1 FROM medico WHERE correo = 'ana.vargas@tezza.test');

UPDATE medico SET nombre=CONVERT(0x46C3A174696D61 USING utf8mb4),
  especialidad=CONVERT(0x4F6674616C6D6F6C6F67C3AD61 USING utf8mb4)
WHERE correo='fatima.robles@tezza.test';
UPDATE medico SET apellido=CONVERT(0x436F6E747265726173204CC3B370657A USING utf8mb4),
  especialidad=CONVERT(0x43617264696F6C6F67C3AD61 USING utf8mb4)
WHERE correo='luis.contreras@tezza.test';
UPDATE medico SET especialidad=CONVERT(0x50656469617472C3AD61 USING utf8mb4)
WHERE correo='ana.vargas@tezza.test';

INSERT IGNORE INTO horario (id_medico, dia, hora_inicio, hora_fin)
SELECT id_medico, 'LUNES', '09:00:00', '13:00:00' FROM medico WHERE correo='fatima.robles@tezza.test';
INSERT IGNORE INTO horario (id_medico, dia, hora_inicio, hora_fin)
SELECT id_medico, 'MIERCOLES', '09:00:00', '13:00:00' FROM medico WHERE correo='fatima.robles@tezza.test';
INSERT IGNORE INTO horario (id_medico, dia, hora_inicio, hora_fin)
SELECT id_medico, 'VIERNES', '09:00:00', '13:00:00' FROM medico WHERE correo='fatima.robles@tezza.test';
INSERT IGNORE INTO horario (id_medico, dia, hora_inicio, hora_fin)
SELECT id_medico, 'MARTES', '14:00:00', '18:00:00' FROM medico WHERE correo='luis.contreras@tezza.test';
INSERT IGNORE INTO horario (id_medico, dia, hora_inicio, hora_fin)
SELECT id_medico, 'JUEVES', '14:00:00', '18:00:00' FROM medico WHERE correo='luis.contreras@tezza.test';
INSERT IGNORE INTO horario (id_medico, dia, hora_inicio, hora_fin)
SELECT id_medico, 'LUNES', '08:00:00', '12:00:00' FROM medico WHERE correo='ana.vargas@tezza.test';
INSERT IGNORE INTO horario (id_medico, dia, hora_inicio, hora_fin)
SELECT id_medico, 'MARTES', '08:00:00', '12:00:00' FROM medico WHERE correo='ana.vargas@tezza.test';
INSERT IGNORE INTO horario (id_medico, dia, hora_inicio, hora_fin)
SELECT id_medico, 'JUEVES', '08:00:00', '12:00:00' FROM medico WHERE correo='ana.vargas@tezza.test';
