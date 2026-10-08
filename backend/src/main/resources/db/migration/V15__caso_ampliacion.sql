-- CU-18 Ampliación de casos: información que el ciudadano agrega cuando el
-- Auxiliar le pide ampliar su caso. No se mezcla con la descripción original
-- para que el Auxiliar pueda distinguir lo que se agregó después.

ALTER TABLE caso ADD COLUMN IF NOT EXISTS informacion_ampliacion VARCHAR(2000);
