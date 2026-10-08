-- CU-06 Bandeja Supervisores: observaciones de la devolución y ruta del reporte
-- de supervisión que se genera al aprobar el reporte de atención del Técnico.

ALTER TABLE caso ADD COLUMN IF NOT EXISTS informe_supervision_ruta VARCHAR(500);
ALTER TABLE caso ADD COLUMN IF NOT EXISTS observaciones_supervision VARCHAR(1000);
