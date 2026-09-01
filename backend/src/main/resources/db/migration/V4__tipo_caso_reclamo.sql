-- Tipo de caso Reclamo: exigencia de solucion ante incumplimiento de lo contratado o comprado.
-- No se altera V2 (checksum ya aplicado). Categorias de conducta/irregularidad siguen fuera de Reclamo.

INSERT INTO categoria_tipo_caso(id_categoria, tipo_caso)
SELECT id, 'Reclamo' FROM categoria WHERE nombre IN (
    'Agua potable', 'Alumbrado publico', 'Recoleccion de basura', 'Drenajes',
    'Sistemas informaticos municipales', 'Portal web y servicios digitales',
    'Licencias y permisos', 'Tramites administrativos',
    'Cobros e impuestos', 'Pagos y arbitrios'
)
ON CONFLICT DO NOTHING;
