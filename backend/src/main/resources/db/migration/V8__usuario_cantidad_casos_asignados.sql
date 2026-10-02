-- Contador de casos "En Atención" que tiene cada usuario (Técnico), para no
-- tener que calcularlo con un COUNT cada vez que se asigna un caso nuevo.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS cantidad_casos_asignados INT NOT NULL DEFAULT 0;

-- Dejamos el contador en el valor correcto según los casos que ya existan.
UPDATE usuario u
SET cantidad_casos_asignados = (
    SELECT COUNT(*)
    FROM caso c
    WHERE c.id_tecnico = u.cui
      AND c.id_estado = (SELECT id FROM estado_caso WHERE estado = 'En Atención')
);
