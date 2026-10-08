-- CU-07 Bandeja Jefaturas: ruta del oficio de resolución firmado y observaciones
-- cuando la Jefatura devuelve el caso al Supervisor.

ALTER TABLE caso ADD COLUMN IF NOT EXISTS oficio_ruta VARCHAR(500);
ALTER TABLE caso ADD COLUMN IF NOT EXISTS observaciones_jefatura VARCHAR(1000);
