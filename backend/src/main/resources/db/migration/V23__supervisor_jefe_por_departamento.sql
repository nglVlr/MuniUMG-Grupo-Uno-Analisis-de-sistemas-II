-- V18 describe un Supervisor y un Jefe activos por departamento, pero los UPDATE
-- buscaban correos que nunca se insertaron (diego.paz@miumg.edu.gt,
-- priscila.tzoc@miumg.edu.gt, edwin.say@miumg.edu.gt, rolando.ixchop@miumg.edu.gt).
-- En V7 esas personas quedaron como dpaz1, ptzoc1, esay1 y rixchop1, todas con
-- rol Tecnico. El resultado en una base migrada desde cero es:
--   Asuntos Municipales y Recursos Humanos sin Supervisor ni Jefe,
--   Servicios Publicos sin Supervisor,
--   Tesoreria sin Jefe.
-- Los casos En Supervisión, Pendiente de Aprobación de Rechazo y Pendiente de
-- Firma de esos departamentos no aparecen en ninguna bandeja (CU-06 y CU-07).
--
-- Se aplica el organigrama que V18 documenta, solo si el departamento todavía
-- no tiene un usuario Activo de ese rol. Los casos abiertos del Técnico que
-- cambia de rol pasan al Técnico que permanece en el departamento, para que no
-- queden fuera de la bandeja de atención.

DO $$
DECLARE
    rec RECORD;
    v_activo BIGINT;
    v_rol_id BIGINT;
    v_cui VARCHAR(13);
    v_depto BIGINT;
    v_destino VARCHAR(13);
    v_ya BOOLEAN;
BEGIN
    SELECT id INTO v_activo FROM estado_usuarios WHERE estado = 'Activo';

    FOR rec IN
        SELECT *
        FROM (VALUES
            ('dpaz1@miumg.edu.gt',      'Supervisor', 'mroquel1@miumg.edu.gt'),
            ('kxitumul1@miumg.edu.gt',  'Jefe',       'mroquel1@miumg.edu.gt'),
            ('ptzoc1@miumg.edu.gt',     'Supervisor', 'fboc1@miumg.edu.gt'),
            ('esay1@miumg.edu.gt',      'Jefe',       'fboc1@miumg.edu.gt'),
            ('jmorales1@miumg.edu.gt',  'Supervisor', 'bsical1@miumg.edu.gt'),
            ('rixchop1@miumg.edu.gt',   'Jefe',       'ccux1@miumg.edu.gt')
        ) AS t(correo, rol_destino, correo_destino)
    LOOP
        SELECT u.cui, u.id_departamento INTO v_cui, v_depto
        FROM usuario u
        WHERE u.correo = rec.correo;

        IF v_cui IS NULL THEN
            CONTINUE;
        END IF;

        SELECT EXISTS (
            SELECT 1
            FROM usuario o
            JOIN rol r ON r.id = o.id_rol
            JOIN estado_usuarios e ON e.id = o.id_estado
            WHERE o.id_departamento = v_depto
              AND r.rol = rec.rol_destino
              AND e.estado = 'Activo'
        ) INTO v_ya;

        IF v_ya THEN
            CONTINUE;
        END IF;

        SELECT cui INTO v_destino FROM usuario WHERE correo = rec.correo_destino;
        IF v_destino IS NOT NULL AND v_destino <> v_cui THEN
            UPDATE caso
            SET id_tecnico = v_destino
            WHERE id_tecnico = v_cui
              AND id_estado IN (
                  SELECT id FROM estado_caso
                  WHERE estado IN (
                      'En Atención',
                      'En Supervisión',
                      'Pendiente de Firma',
                      'Requiere Ampliación'
                  )
              );
        END IF;

        SELECT id INTO v_rol_id FROM rol WHERE rol = rec.rol_destino;
        UPDATE usuario
        SET id_rol = v_rol_id,
            id_estado = v_activo
        WHERE cui = v_cui;
    END LOOP;

    UPDATE usuario u
    SET cantidad_casos_asignados = (
        SELECT COUNT(*)::INT
        FROM caso c
        JOIN estado_caso e ON e.id = c.id_estado
        WHERE c.id_tecnico = u.cui
          AND e.estado = 'En Atención'
    );
END $$;
