-- CU-11 Administración de Catálogos: el Admin puede inactivar un departamento
-- que ya no se usa, igual que hace con categorías y motivos de rechazo.

ALTER TABLE departamento ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
