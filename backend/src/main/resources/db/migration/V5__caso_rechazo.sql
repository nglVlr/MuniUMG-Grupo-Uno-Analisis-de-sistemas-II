-- CU-04 Bandeja Auxiliar: motivo y comentario cuando el Auxiliar rechaza un caso
ALTER TABLE caso ADD COLUMN IF NOT EXISTS id_motivo_rechazo BIGINT REFERENCES motivo_rechazo(id);
ALTER TABLE caso ADD COLUMN IF NOT EXISTS comentario_rechazo VARCHAR(500);
