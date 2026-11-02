-- CU-19 Dashboard de graficas: completa los casos de demostracion hasta 350.
-- La V20 y la V21 reparten los casos entre todos los departamentos por turno, y
-- saltan el turno cuando al departamento no le quedan categorias activas. En las
-- instalaciones con un departamento sin categorias activas quedaron menos casos de
-- los previstos, asi que esta migracion genera los que faltan usando solo los
-- departamentos que si pueden recibirlos.
--
-- Es idempotente: si los ciudadanos de demostracion ya suman 350 casos, no hace nada.
-- En una instalacion desde cero la V20 ya dejo los 350 y esta migracion no toca nada.
-- No agrega inicios de sesion ni sesiones fallidas: esos ya los dejaron la V20 y la V21.

DO $demo$
DECLARE
    v_cargados      INT;
    v_total         INT;
    v_meta          INT := 350;
    v_faltan        INT;
    v_departamentos INT;
    v_estados       TEXT[];
    v_direcciones   TEXT[] := ARRAY[
        '2a calle 3-45 zona 1, Granados',
        'Aldea El Rejón, camino principal',
        'Barrio La Cruz, frente al mercado',
        'Caserío Santa Rosa, sector norte',
        'Colonia Las Flores, lote 18',
        '4a avenida 7-12 zona 2, Granados',
        'Aldea Pacul, entrada principal',
        'Barrio El Centro, atrás de la escuela',
        'Camino a El Tablón, kilómetro 3',
        'Cantón San Antonio, calle del cementerio'];
    v_descripciones TEXT[] := ARRAY[
        'El servicio lleva varios días con fallas y nadie ha llegado a revisarlo.',
        'Se solicita revisión del problema reportado porque afecta a todo el sector.',
        'El personal no atendió la solicitud presentada la semana pasada.',
        'La situación se repite cada mes y no se ha dado ninguna solución.',
        'Se reporta la falta del servicio en el sector desde hace varios días.',
        'Se pide que se corrija el cobro aplicado porque no corresponde.',
        'La infraestructura presenta daños que ponen en riesgo a los vecinos.',
        'Se sugiere ampliar el horario de atención para los vecinos del área rural.',
        'El trámite presentado no ha recibido respuesta en el plazo indicado.',
        'Se denuncia una irregularidad observada durante la atención al público.'];
    v_acciones      TEXT[];
    v_actores       TEXT[];
    v_dpis          TEXT[];
    v_id_rol_tec    BIGINT;
    v_id_activo     BIGINT;
    v_auxiliar      TEXT;
    v_dep           BIGINT;
    v_cat           BIGINT;
    v_tipo          TEXT;
    v_tecnico       TEXT;
    v_supervisor    TEXT;
    v_jefe          TEXT;
    v_dpi           TEXT;
    v_estado        TEXT;
    v_id_estado     BIGINT;
    v_motivo        BIGINT;
    v_codigo        TEXT;
    v_correlativo   INT := 8000;
    v_id_caso       BIGINT;
    v_create        TIMESTAMP;
    v_hora          INT;
    v_edad          NUMERIC;
    v_span          NUMERIC;
    v_momento       TIMESTAMP;
    v_previo        TEXT;
    v_nuevo         TEXT;
    v_actor_cui     TEXT;
    v_actor_rol     TEXT;
    v_ip            TEXT;
    v_detalle       TEXT;
    v_correo        TEXT;
    v_rol_texto     TEXT;
    v_actividades   INT;
    i               INT;
    j               INT;
BEGIN
    -- Cuantos casos de demostracion hay ya, y cuantos faltan para llegar a 350
    SELECT count(*) INTO v_cargados
    FROM caso
    WHERE dpi IN ('2800000000001','2800000000002','2800000000003','2800000000004',
                  '2800000000005','2800000000006','2800000000007','2800000000008',
                  '2800000000009','2800000000010','2800000000011','2800000000012');

    v_faltan := v_meta - v_cargados;
    IF v_faltan <= 0 THEN
        RAISE NOTICE 'Los datos de demostracion ya llegan a % casos, no falta nada', v_cargados;
        RETURN;
    END IF;
    v_total := v_faltan;

    -- Los departamentos sin categorias activas no pueden recibir casos
    SELECT count(*) INTO v_departamentos
    FROM departamento d
    WHERE EXISTS (SELECT 1 FROM categoria c WHERE c.id_departamento = d.id AND c.activo = TRUE);

    IF v_departamentos = 0 THEN
        RAISE NOTICE 'Ningun departamento tiene categorias activas, no se generan casos';
        RETURN;
    END IF;

    RAISE NOTICE 'Se completan % casos de demostracion para llegar a %', v_faltan, v_meta;

    -- Semilla fija: los datos de demostración salen iguales en cada instalación
    PERFORM setseed(0.7731);

    SELECT id INTO v_id_rol_tec FROM rol WHERE rol = 'Tecnico';
    SELECT id INTO v_id_activo FROM estado_usuarios WHERE estado = 'Activo';

    SELECT cui INTO v_auxiliar
    FROM usuario
    WHERE id_rol = (SELECT id FROM rol WHERE rol = 'Auxiliar') AND id_estado = v_id_activo
    ORDER BY cui LIMIT 1;

    IF v_auxiliar IS NULL THEN
        RAISE NOTICE 'No hay Auxiliar activo, no se pueden generar los datos de demostracion';
        RETURN;
    END IF;

    v_dpis := ARRAY[
        '2800000000001','2800000000002','2800000000003','2800000000004',
        '2800000000005','2800000000006','2800000000007','2800000000008',
        '2800000000009','2800000000010','2800000000011','2800000000012'];

    -- Reparto de los 350 casos entre los nueve estados
    v_estados :=
        array_fill('Resuelto'::text, ARRAY[110]) ||
        array_fill('Rechazado'::text, ARRAY[35]) ||
        array_fill('Requiere Ampliación'::text, ARRAY[25]) ||
        array_fill('Registrado'::text, ARRAY[30]) ||
        array_fill('En Revisión'::text, ARRAY[35]) ||
        array_fill('En Atención'::text, ARRAY[45]) ||
        array_fill('En Supervisión'::text, ARRAY[30]) ||
        array_fill('Pendiente de Firma'::text, ARRAY[25]) ||
        array_fill('Pendiente de Aprobación de Rechazo'::text, ARRAY[15]);

    FOR i IN 1..v_total LOOP
        -- Se recorre el reparto completo a pasos parejos, para que los casos que
        -- faltan mantengan la misma proporcion entre estados
        v_estado := v_estados[1 + ((i - 1) * v_meta) / v_total];
        SELECT id INTO v_id_estado FROM estado_caso WHERE estado = v_estado;

        -- Departamento por turno, solo entre los que tienen categorias activas
        SELECT d.id INTO v_dep
        FROM departamento d
        WHERE EXISTS (SELECT 1 FROM categoria c WHERE c.id_departamento = d.id AND c.activo = TRUE)
        ORDER BY d.id
        OFFSET (i % v_departamentos) LIMIT 1;

        -- Categoría del departamento y tipo de caso que esa categoría admite
        SELECT c.id INTO v_cat
        FROM categoria c
        WHERE c.id_departamento = v_dep AND c.activo = TRUE
        ORDER BY random() LIMIT 1;

        IF v_cat IS NULL THEN
            CONTINUE;
        END IF;

        SELECT tipo_caso INTO v_tipo
        FROM categoria_tipo_caso
        WHERE id_categoria = v_cat
        ORDER BY random() LIMIT 1;

        IF v_tipo IS NULL THEN
            v_tipo := 'Queja';
        END IF;

        -- Responsables del departamento. Si al departamento le falta alguno,
        -- se toma cualquiera activo de ese rol para no dejar la bitácora incompleta.
        SELECT cui INTO v_tecnico FROM usuario
        WHERE id_rol = v_id_rol_tec AND id_departamento = v_dep AND id_estado = v_id_activo
        ORDER BY random() LIMIT 1;
        IF v_tecnico IS NULL THEN
            SELECT cui INTO v_tecnico FROM usuario
            WHERE id_rol = v_id_rol_tec AND id_estado = v_id_activo ORDER BY random() LIMIT 1;
        END IF;

        SELECT cui INTO v_supervisor FROM usuario
        WHERE id_rol = (SELECT id FROM rol WHERE rol = 'Supervisor')
          AND id_departamento = v_dep AND id_estado = v_id_activo
        ORDER BY cui LIMIT 1;
        IF v_supervisor IS NULL THEN
            SELECT cui INTO v_supervisor FROM usuario
            WHERE id_rol = (SELECT id FROM rol WHERE rol = 'Supervisor') AND id_estado = v_id_activo
            ORDER BY cui LIMIT 1;
        END IF;

        SELECT cui INTO v_jefe FROM usuario
        WHERE id_rol = (SELECT id FROM rol WHERE rol = 'Jefe')
          AND id_departamento = v_dep AND id_estado = v_id_activo
        ORDER BY cui LIMIT 1;
        IF v_jefe IS NULL THEN
            SELECT cui INTO v_jefe FROM usuario
            WHERE id_rol = (SELECT id FROM rol WHERE rol = 'Jefe') AND id_estado = v_id_activo
            ORDER BY cui LIMIT 1;
        END IF;

        v_dpi := v_dpis[1 + floor(random() * array_length(v_dpis, 1))::int];

        -- La mayoría de los casos entra en horario de oficina y el resto a cualquier hora
        IF random() < 0.2 THEN
            v_hora := floor(random() * 24)::int;
        ELSE
            v_hora := 7 + floor(random() * 11)::int;
        END IF;

        v_create := date_trunc('day', now())
                    - (2 + floor(random() * 358))::int * interval '1 day'
                    + v_hora * interval '1 hour'
                    + (floor(random() * 60))::int * interval '1 minute';

        -- El correlativo avanza hasta dar con un codigo libre, por si alguno de la
        -- banda 8000 ya se hubiera usado a mano
        LOOP
            v_correlativo := v_correlativo + 1;
            v_codigo := 'MG-' || to_char(v_correlativo, 'FM0000') || '-' || to_char(v_create, 'YYYY');
            EXIT WHEN NOT EXISTS (SELECT 1 FROM caso WHERE codigo_seguimiento = v_codigo);
        END LOOP;
        v_ip := '190.' || (50 + floor(random() * 30))::int || '.'
                || floor(random() * 255)::int || '.' || (1 + floor(random() * 254))::int;

        -- El motivo de rechazo solo existe cuando el caso se rechazó
        v_motivo := NULL;
        IF v_estado IN ('Rechazado', 'Requiere Ampliación', 'Pendiente de Aprobación de Rechazo') THEN
            SELECT id INTO v_motivo FROM motivo_rechazo WHERE activo = TRUE ORDER BY random() LIMIT 1;
        END IF;

        INSERT INTO caso(codigo_seguimiento, dpi, tipo_caso, id_categoria, id_departamento,
                         direccion, descripcion, id_estado, ip_origen, create_at, id_tecnico,
                         id_motivo_rechazo, comentario_rechazo)
        VALUES (v_codigo, v_dpi, v_tipo, v_cat, v_dep,
                v_direcciones[1 + floor(random() * array_length(v_direcciones, 1))::int],
                v_descripciones[1 + floor(random() * array_length(v_descripciones, 1))::int],
                v_id_estado, v_ip, v_create,
                CASE WHEN v_estado IN ('En Atención', 'En Supervisión', 'Pendiente de Firma', 'Resuelto')
                     THEN v_tecnico ELSE NULL END,
                v_motivo,
                CASE WHEN v_motivo IS NULL THEN NULL
                     ELSE 'El caso no cumple con los requisitos para continuar el trámite.' END)
        RETURNING id INTO v_id_caso;

        -- Secuencia de acciones que deja el caso en su estado actual
        IF v_estado = 'Registrado' THEN
            v_acciones := ARRAY['Registro de caso'];
            v_actores := ARRAY['ciudadano'];
        ELSIF v_estado = 'En Revisión' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso'];
            v_actores := ARRAY['ciudadano', 'auxiliar'];
        ELSIF v_estado = 'En Atención' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Asignación de Técnico'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar'];
        ELSIF v_estado = 'En Supervisión' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Asignación de Técnico',
                                'Finalización de atención'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar', 'tecnico'];
        ELSIF v_estado = 'Pendiente de Firma' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Asignación de Técnico',
                                'Finalización de atención', 'Aprobación de reporte de atención'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar', 'tecnico', 'supervisor'];
        ELSIF v_estado = 'Resuelto' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Asignación de Técnico',
                                'Finalización de atención'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar', 'tecnico'];
            -- Una parte de los casos regresa al Técnico antes de aprobarse
            IF random() < 0.25 THEN
                v_acciones := v_acciones || ARRAY['Devolución al Técnico', 'Finalización de atención'];
                v_actores := v_actores || ARRAY['supervisor', 'tecnico'];
            END IF;
            v_acciones := v_acciones || ARRAY['Aprobación de reporte de atención'];
            v_actores := v_actores || ARRAY['supervisor'];
            -- Y otra parte la devuelve la Jefatura a Supervisión
            IF random() < 0.12 THEN
                v_acciones := v_acciones || ARRAY['Devolución a Supervisión',
                                                  'Aprobación de reporte de atención'];
                v_actores := v_actores || ARRAY['jefe', 'supervisor'];
            END IF;
            v_acciones := v_acciones || ARRAY['Firma de oficio de resolución'];
            v_actores := v_actores || ARRAY['jefe'];
        ELSIF v_estado = 'Pendiente de Aprobación de Rechazo' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Rechazo de caso'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar'];
        ELSIF v_estado = 'Rechazado' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Rechazo de caso',
                                'Aprobación de rechazo'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar', 'supervisor'];
        ELSE
            -- Requiere Ampliación: el Auxiliar le pide al ciudadano ampliar su caso
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Rechazo con ampliación'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar'];
        END IF;

        -- Los pasos se reparten dentro del tiempo que el caso lleva abierto,
        -- así ninguna fecha de la bitácora queda en el futuro
        v_edad := EXTRACT(EPOCH FROM (now() - v_create)) / 86400.0;
        v_span := LEAST(1 + random() * 29, GREATEST(v_edad * 0.9, 0.05));
        v_previo := NULL;

        FOR j IN 1..array_length(v_acciones, 1) LOOP
            v_momento := v_create + (v_span * (j - 1) / GREATEST(array_length(v_acciones, 1) - 1, 1))
                         * interval '1 day';

            IF v_actores[j] = 'ciudadano' THEN
                v_actor_cui := v_dpi;
                v_actor_rol := 'Ciudadano';
            ELSIF v_actores[j] = 'auxiliar' THEN
                v_actor_cui := v_auxiliar;
                v_actor_rol := 'Auxiliar';
            ELSIF v_actores[j] = 'tecnico' THEN
                v_actor_cui := v_tecnico;
                v_actor_rol := 'Tecnico';
            ELSIF v_actores[j] = 'supervisor' THEN
                v_actor_cui := v_supervisor;
                v_actor_rol := 'Supervisor';
            ELSE
                v_actor_cui := v_jefe;
                v_actor_rol := 'Jefe';
            END IF;

            -- Estado al que pasa el caso con esa acción
            v_nuevo := CASE v_acciones[j]
                WHEN 'Registro de caso' THEN 'Registrado'
                WHEN 'Aceptación de caso' THEN 'En Revisión'
                WHEN 'Asignación de Técnico' THEN 'En Atención'
                WHEN 'Finalización de atención' THEN 'En Supervisión'
                WHEN 'Aprobación de reporte de atención' THEN 'Pendiente de Firma'
                WHEN 'Firma de oficio de resolución' THEN 'Resuelto'
                WHEN 'Devolución al Técnico' THEN 'En Atención'
                WHEN 'Devolución a Supervisión' THEN 'En Supervisión'
                WHEN 'Rechazo de caso' THEN 'Pendiente de Aprobación de Rechazo'
                WHEN 'Rechazo con ampliación' THEN 'Requiere Ampliación'
                WHEN 'Aprobación de rechazo' THEN 'Rechazado'
                ELSE NULL END;

            v_detalle := CASE v_acciones[j]
                WHEN 'Registro de caso' THEN 'Caso registrado por el ciudadano'
                WHEN 'Aceptación de caso' THEN 'El Auxiliar aceptó el caso y pasó a revisión'
                WHEN 'Asignación de Técnico' THEN 'El caso se asignó al Técnico del departamento'
                WHEN 'Finalización de atención' THEN 'El Técnico finalizó la atención del caso'
                WHEN 'Aprobación de reporte de atención' THEN 'El Supervisor aprobó el reporte de atención'
                WHEN 'Firma de oficio de resolución' THEN 'La Jefatura firmó el oficio de resolución'
                WHEN 'Devolución al Técnico' THEN 'El Supervisor devolvió el caso al Técnico'
                WHEN 'Devolución a Supervisión' THEN 'La Jefatura devolvió el caso a Supervisión'
                WHEN 'Rechazo de caso' THEN 'El Auxiliar solicitó el rechazo del caso'
                WHEN 'Rechazo con ampliación' THEN 'El Auxiliar solicitó ampliar la información del caso'
                WHEN 'Aprobación de rechazo' THEN 'El Supervisor aprobó el rechazo y el caso pasó a Rechazado'
                ELSE 'Cambio registrado en el caso' END;

            INSERT INTO bitacora_cambio(accion, usuario, id_caso, fecha, hora, ip_origen,
                                        detalle, rol, entidad, valor_anterior, valor_nuevo)
            VALUES (v_acciones[j], v_actor_cui, v_id_caso, v_momento::date,
                    date_trunc('second', v_momento)::time, v_ip, v_detalle, v_actor_rol,
                    'Caso', v_previo, v_nuevo);

            v_previo := v_nuevo;
        END LOOP;

        -- Actividades del Técnico, solo en los casos que llegaron a atenderse
        IF v_estado IN ('En Atención', 'En Supervisión', 'Pendiente de Firma', 'Resuelto')
           AND v_tecnico IS NOT NULL THEN
            v_actividades := 1 + floor(random() * 4)::int;
            FOR j IN 1..v_actividades LOOP
                v_momento := v_create + (v_span * j / (v_actividades + 1)) * interval '1 day';
                INSERT INTO actividad_caso(id_caso, id_tecnico, detalle, fecha)
                VALUES (v_id_caso, v_tecnico,
                        'Visita de campo y verificación del problema reportado, avance '
                        || j || ' de ' || v_actividades,
                        v_momento);

                INSERT INTO bitacora_cambio(accion, usuario, id_caso, fecha, hora, ip_origen,
                                            detalle, rol, entidad, valor_anterior, valor_nuevo)
                VALUES ('Registro de actividad', v_tecnico, v_id_caso, v_momento::date,
                        date_trunc('second', v_momento)::time, v_ip,
                        'El Técnico registró una actividad del caso', 'Tecnico', 'Caso', NULL, NULL);
            END LOOP;
        END IF;

        -- Notificación del registro del caso; unas pocas quedan con error
        IF random() < 0.05 THEN
            INSERT INTO notificacion(evento, destinatario, cuerpo, resultado, intentos, id_caso, create_at)
            SELECT 'registro del caso', c.correo,
                   'Su caso ' || v_codigo || ' fue registrado.', 'ERROR', 3, v_id_caso, v_create
            FROM ciudadano c WHERE c.dpi = v_dpi;

            INSERT INTO bitacora_cambio(accion, usuario, id_caso, fecha, hora, ip_origen,
                                        detalle, rol, entidad, valor_anterior, valor_nuevo)
            VALUES ('Error envío de notificación', v_auxiliar, v_id_caso, v_create::date,
                    date_trunc('second', v_create)::time, v_ip,
                    'Evento registro del caso: no se pudo enviar el correo',
                    'Auxiliar', 'Caso', NULL, NULL);
        ELSE
            INSERT INTO notificacion(evento, destinatario, cuerpo, resultado, intentos, id_caso, create_at)
            SELECT 'registro del caso', c.correo,
                   'Su caso ' || v_codigo || ' fue registrado.', 'ENVIADA', 1, v_id_caso, v_create
            FROM ciudadano c WHERE c.dpi = v_dpi;
        END IF;
    END LOOP;

    RAISE NOTICE 'Casos de demostracion completados';
END
$demo$;

-- RN: el contador de casos asignados del Tecnico debe cuadrar con sus casos En Atencion
UPDATE usuario u
SET cantidad_casos_asignados = (
    SELECT COUNT(*)
    FROM caso c
    WHERE c.id_tecnico = u.cui
      AND c.id_estado = (SELECT id FROM estado_caso WHERE estado = 'En Atención')
)
WHERE u.id_rol = (SELECT id FROM rol WHERE rol = 'Tecnico');
