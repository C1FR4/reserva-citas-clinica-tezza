-- Después de permitir id_usuario NULL en medico, elimina el vínculo provisional
-- creado por el seed inicial entre la cuenta admin y el perfil médico.
USE clinica_tezza;
UPDATE medico m
JOIN usuario u ON u.id_usuario = m.id_usuario
SET m.id_usuario = NULL
WHERE u.rol = 'ADMINISTRADOR';
