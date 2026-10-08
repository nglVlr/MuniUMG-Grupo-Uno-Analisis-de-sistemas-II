-- CU-15 Bitácora de actividades: unifica la bitácora de sesiones y accesos con la
-- de cambios y auditoría. Se amplía la tabla que ya existe en lugar de crear una
-- segunda, para no partir el historial en dos ni duplicar los registros.

ALTER TABLE bitacora_cambio ADD COLUMN IF NOT EXISTS rol VARCHAR(50);
ALTER TABLE bitacora_cambio ADD COLUMN IF NOT EXISTS entidad VARCHAR(50);
ALTER TABLE bitacora_cambio ADD COLUMN IF NOT EXISTS valor_anterior VARCHAR(250);
ALTER TABLE bitacora_cambio ADD COLUMN IF NOT EXISTS valor_nuevo VARCHAR(250);

-- La consulta del Admin filtra por fecha, usuario y acción
CREATE INDEX IF NOT EXISTS idx_bitacora_fecha ON bitacora_cambio(fecha);
CREATE INDEX IF NOT EXISTS idx_bitacora_usuario ON bitacora_cambio(usuario);
CREATE INDEX IF NOT EXISTS idx_bitacora_accion ON bitacora_cambio(accion);
