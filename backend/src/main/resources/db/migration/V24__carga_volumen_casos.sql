-- Carga de volumen para pruebas de bandejas, reportería, bitácora y dashboard.
-- Agrega 150 ciudadanos y 800 casos (códigos MG-7xxx-año, fuera del correlativo
-- real y de la demostración MG-9xxx), con bitácora, actividades y notificaciones.
-- Idempotente: si ya existe un caso MG-7%, no vuelve a insertar.

INSERT INTO ciudadano(dpi, nombre, telefono, correo)
SELECT
    '29' || lpad(i::text, 11, '0'),
    (ARRAY['Ana','Luis','Marta','Carlos','Sofía','Pedro','Lucía','Jorge','Elena','Diego',
           'Rosa','Hugo','Carmen','Andrés','Patricia','Raúl','Gloria','Oscar','Irene','Pablo'])[1 + ((i - 1) % 20)]
        || ' '
        || (ARRAY['Ramos','Tzul','Yaxón','Chávez','Calel','Ajpop','Pacay','Siney','Toj','Yool',
                  'Jocol','Chun','Quej','Coc','Pérez','López','Sum','Mejía','Reyes','Marroquín'])[1 + ((i - 1) % 20)]
        || ' '
        || (ARRAY['Quej','Batz','Canil','Ixcoy','Barrios','López','García','Morales','Cruz','Díaz'])[1 + ((i - 1) % 10)],
    lpad((55030000 + i)::text, 8, '0'),
    'ciudadano.vol' || lpad(i::text, 3, '0') || '@ejemplo.gt'
FROM generate_series(1, 150) AS i
ON CONFLICT (dpi) DO NOTHING;

DO $vol$
DECLARE
    v_total         INT := 800;
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
        'Cantón San Antonio, calle del cementerio',
        'Aldea El Arco, sector 2',
        'Barrio San José, calle de la iglesia',
        'Caserío Los Pinos, lote 4',
        'Colonia El Maestro, casa 9',
        'Camino real hacia Rabinal, km 6'];
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
    v_n_dep         INT;
    i               INT;
    j               INT;
BEGIN
    IF EXISTS (SELECT 1 FROM caso WHERE codigo_seguimiento LIKE 'MG-7%') THEN
        RAISE NOTICE 'La carga de volumen MG-7 ya estaba aplicada';
        RETURN;
    END IF;

    PERFORM setseed(0.2404);

    SELECT count(*) INTO v_n_dep FROM departamento;
    SELECT id INTO v_id_rol_tec FROM rol WHERE rol = 'Tecnico';
    SELECT id INTO v_id_activo FROM estado_usuarios WHERE estado = 'Activo';

    SELECT cui INTO v_auxiliar
    FROM usuario
    WHERE id_rol = (SELECT id FROM rol WHERE rol = 'Auxiliar')
      AND id_estado = v_id_activo
    ORDER BY cui
    LIMIT 1;

    IF v_auxiliar IS NULL OR v_n_dep = 0 THEN
        RAISE NOTICE 'No hay auxiliar o departamentos activos, no se carga el volumen';
        RETURN;
    END IF;

    v_estados :=
        array_fill('Resuelto'::text, ARRAY[280]) ||
        array_fill('Rechazado'::text, ARRAY[80]) ||
        array_fill('Requiere Ampliación'::text, ARRAY[50]) ||
        array_fill('Registrado'::text, ARRAY[70]) ||
        array_fill('En Revisión'::text, ARRAY[70]) ||
        array_fill('En Atención'::text, ARRAY[100]) ||
        array_fill('En Supervisión'::text, ARRAY[60]) ||
        array_fill('Pendiente de Firma'::text, ARRAY[50]) ||
        array_fill('Pendiente de Aprobación de Rechazo'::text, ARRAY[40]);

    FOR i IN 1..v_total LOOP
        v_estado := v_estados[i];
        SELECT id INTO v_id_estado FROM estado_caso WHERE estado = v_estado;

        SELECT id INTO v_dep
        FROM departamento
        ORDER BY id
        OFFSET (i % v_n_dep) LIMIT 1;

        SELECT c.id INTO v_cat
        FROM categoria c
        WHERE c.id_departamento = v_dep AND c.activo = TRUE
        ORDER BY c.id
        OFFSET (i % GREATEST((SELECT count(*) FROM categoria x WHERE x.id_departamento = v_dep AND x.activo), 1))
        LIMIT 1;

        IF v_cat IS NULL THEN
            CONTINUE;
        END IF;

        SELECT tipo_caso INTO v_tipo
        FROM categoria_tipo_caso
        WHERE id_categoria = v_cat
        ORDER BY tipo_caso
        OFFSET (i % GREATEST((SELECT count(*) FROM categoria_tipo_caso t WHERE t.id_categoria = v_cat), 1))
        LIMIT 1;

        IF v_tipo IS NULL THEN
            v_tipo := 'Queja';
        END IF;

        SELECT cui INTO v_tecnico FROM usuario
        WHERE id_rol = v_id_rol_tec AND id_departamento = v_dep AND id_estado = v_id_activo
        ORDER BY cui
        OFFSET (i % GREATEST((
            SELECT count(*) FROM usuario u
            WHERE u.id_rol = v_id_rol_tec AND u.id_departamento = v_dep AND u.id_estado = v_id_activo
        ), 1))
        LIMIT 1;

        IF v_tecnico IS NULL THEN
            SELECT cui INTO v_tecnico FROM usuario
            WHERE id_rol = v_id_rol_tec AND id_estado = v_id_activo
            ORDER BY cui LIMIT 1;
        END IF;

        SELECT cui INTO v_supervisor FROM usuario
        WHERE id_rol = (SELECT id FROM rol WHERE rol = 'Supervisor')
          AND id_departamento = v_dep AND id_estado = v_id_activo
        ORDER BY cui LIMIT 1;

        SELECT cui INTO v_jefe FROM usuario
        WHERE id_rol = (SELECT id FROM rol WHERE rol = 'Jefe')
          AND id_departamento = v_dep AND id_estado = v_id_activo
        ORDER BY cui LIMIT 1;

        v_dpi := '29' || lpad((1 + ((i - 1) % 150))::text, 11, '0');

        IF i % 5 = 0 THEN
            v_hora := i % 24;
        ELSE
            v_hora := 7 + (i % 11);
        END IF;

        v_create := date_trunc('day', now())
                    - (3 + (i % 400))::int * interval '1 day'
                    + v_hora * interval '1 hour'
                    + ((i * 7) % 60) * interval '1 minute';

        v_codigo := 'MG-' || to_char(7000 + i, 'FM0000') || '-' || to_char(v_create, 'YYYY');
        v_ip := '190.' || (10 + (i % 40))::text || '.' || ((i * 3) % 255)::text || '.' || (1 + (i % 200))::text;

        v_motivo := NULL;
        IF v_estado IN ('Rechazado', 'Requiere Ampliación', 'Pendiente de Aprobación de Rechazo') THEN
            SELECT id INTO v_motivo FROM motivo_rechazo WHERE activo = TRUE ORDER BY id OFFSET (i % 10) LIMIT 1;
        END IF;

        INSERT INTO caso(
            codigo_seguimiento, dpi, tipo_caso, id_categoria, id_departamento,
            direccion, descripcion, id_estado, ip_origen, create_at, id_tecnico,
            id_motivo_rechazo, comentario_rechazo, informacion_ampliacion
        ) VALUES (
            v_codigo, v_dpi, v_tipo, v_cat, v_dep,
            v_direcciones[1 + ((i - 1) % array_length(v_direcciones, 1))],
            v_descripciones[1 + ((i - 1) % array_length(v_descripciones, 1))]
                || ' Referencia de volumen ' || v_codigo || '.',
            v_id_estado, v_ip, v_create,
            CASE WHEN v_estado IN ('En Atención', 'En Supervisión', 'Pendiente de Firma', 'Resuelto')
                 THEN v_tecnico ELSE NULL END,
            v_motivo,
            CASE WHEN v_motivo IS NULL THEN NULL
                 ELSE 'Información insuficiente para continuar el trámite ' || v_codigo || '.' END,
            CASE WHEN v_estado = 'Requiere Ampliación'
                 THEN 'Se solicita al ciudadano ampliar la descripción y la ubicación del caso ' || v_codigo || '.'
                 ELSE NULL END
        ) RETURNING id INTO v_id_caso;

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
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Asignación de Técnico', 'Finalización de atención'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar', 'tecnico'];
        ELSIF v_estado = 'Pendiente de Firma' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Asignación de Técnico',
                                'Finalización de atención', 'Aprobación de reporte de atención'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar', 'tecnico', 'supervisor'];
        ELSIF v_estado = 'Resuelto' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Asignación de Técnico',
                                'Finalización de atención', 'Aprobación de reporte de atención',
                                'Firma de oficio de resolución'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar', 'tecnico', 'supervisor', 'jefe'];
        ELSIF v_estado = 'Pendiente de Aprobación de Rechazo' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Rechazo de caso'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar'];
        ELSIF v_estado = 'Rechazado' THEN
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Rechazo de caso', 'Aprobación de rechazo'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar', 'supervisor'];
        ELSE
            v_acciones := ARRAY['Registro de caso', 'Aceptación de caso', 'Rechazo con ampliación'];
            v_actores := ARRAY['ciudadano', 'auxiliar', 'auxiliar'];
        END IF;

        v_edad := EXTRACT(EPOCH FROM (now() - v_create)) / 86400.0;
        v_span := LEAST(1 + (i % 20), GREATEST(v_edad * 0.85, 0.05));
        v_previo := NULL;

        FOR j IN 1..array_length(v_acciones, 1) LOOP
            v_momento := v_create + (v_span * (j - 1) / GREATEST(array_length(v_acciones, 1) - 1, 1)) * interval '1 day';

            IF v_actores[j] = 'ciudadano' THEN
                v_actor_cui := v_dpi;
                v_actor_rol := 'Ciudadano';
            ELSIF v_actores[j] = 'auxiliar' THEN
                v_actor_cui := v_auxiliar;
                v_actor_rol := 'Auxiliar';
            ELSIF v_actores[j] = 'tecnico' THEN
                v_actor_cui := COALESCE(v_tecnico, v_auxiliar);
                v_actor_rol := 'Tecnico';
            ELSIF v_actores[j] = 'supervisor' THEN
                v_actor_cui := COALESCE(v_supervisor, v_auxiliar);
                v_actor_rol := 'Supervisor';
            ELSE
                v_actor_cui := COALESCE(v_jefe, v_auxiliar);
                v_actor_rol := 'Jefe';
            END IF;

            v_nuevo := CASE v_acciones[j]
                WHEN 'Registro de caso' THEN 'Registrado'
                WHEN 'Aceptación de caso' THEN 'En Revisión'
                WHEN 'Asignación de Técnico' THEN 'En Atención'
                WHEN 'Finalización de atención' THEN 'En Supervisión'
                WHEN 'Aprobación de reporte de atención' THEN 'Pendiente de Firma'
                WHEN 'Firma de oficio de resolución' THEN 'Resuelto'
                WHEN 'Rechazo de caso' THEN 'Pendiente de Aprobación de Rechazo'
                WHEN 'Rechazo con ampliación' THEN 'Requiere Ampliación'
                WHEN 'Aprobación de rechazo' THEN 'Rechazado'
                ELSE NULL END;

            v_detalle := left(v_acciones[j] || ' del caso ' || v_codigo, 500);

            INSERT INTO bitacora_cambio(
                accion, usuario, id_caso, fecha, hora, ip_origen,
                detalle, rol, entidad, valor_anterior, valor_nuevo
            ) VALUES (
                v_acciones[j], v_actor_cui, v_id_caso, v_momento::date,
                date_trunc('second', v_momento)::time, v_ip, v_detalle, v_actor_rol,
                'Caso', v_previo, v_nuevo
            );

            v_previo := v_nuevo;
        END LOOP;

        IF v_estado IN ('En Atención', 'En Supervisión', 'Pendiente de Firma', 'Resuelto')
           AND v_tecnico IS NOT NULL THEN
            v_actividades := 1 + (i % 4);
            FOR j IN 1..v_actividades LOOP
                v_momento := v_create + (v_span * j / (v_actividades + 1)) * interval '1 day';
                INSERT INTO actividad_caso(id_caso, id_tecnico, detalle, fecha)
                VALUES (
                    v_id_caso, v_tecnico,
                    left('Visita ' || j || ' de ' || v_actividades || ' del caso ' || v_codigo
                         || '. Se verificó el problema en campo.', 500),
                    v_momento
                );
                INSERT INTO bitacora_cambio(
                    accion, usuario, id_caso, fecha, hora, ip_origen,
                    detalle, rol, entidad, valor_anterior, valor_nuevo
                ) VALUES (
                    'Registro de actividad', v_tecnico, v_id_caso, v_momento::date,
                    date_trunc('second', v_momento)::time, v_ip,
                    'El Técnico registró una actividad del caso', 'Tecnico', 'Caso', NULL, NULL
                );
            END LOOP;
        END IF;

        IF i % 25 = 0 THEN
            INSERT INTO notificacion(evento, destinatario, cuerpo, resultado, intentos, id_caso, create_at)
            SELECT 'registro del caso', c.correo,
                   'Su caso ' || v_codigo || ' fue registrado.', 'ERROR', 3, v_id_caso, v_create
            FROM ciudadano c WHERE c.dpi = v_dpi;

            INSERT INTO bitacora_cambio(
                accion, usuario, id_caso, fecha, hora, ip_origen,
                detalle, rol, entidad, valor_anterior, valor_nuevo
            ) VALUES (
                'Error envío de notificación', v_auxiliar, v_id_caso, v_create::date,
                date_trunc('second', v_create)::time, v_ip,
                'Evento registro del caso: no se pudo enviar el correo',
                'Auxiliar', 'Caso', NULL, NULL
            );
        ELSE
            INSERT INTO notificacion(evento, destinatario, cuerpo, resultado, intentos, id_caso, create_at)
            SELECT 'registro del caso', c.correo,
                   'Su caso ' || v_codigo || ' fue registrado.', 'ENVIADA', 1, v_id_caso, v_create
            FROM ciudadano c WHERE c.dpi = v_dpi;
        END IF;

        IF v_estado = 'Resuelto' THEN
            INSERT INTO notificacion(evento, destinatario, cuerpo, resultado, intentos, id_caso, create_at)
            SELECT 'resolución del caso', c.correo,
                   'Su caso ' || v_codigo || ' fue resuelto.', 'ENVIADA', 1, v_id_caso, v_create + interval '2 day'
            FROM ciudadano c WHERE c.dpi = v_dpi;
        ELSIF v_estado = 'Rechazado' THEN
            INSERT INTO notificacion(evento, destinatario, cuerpo, resultado, intentos, id_caso, create_at)
            SELECT 'rechazo del caso', c.correo,
                   'Su caso ' || v_codigo || ' quedó en estado Rechazado.', 'ENVIADA', 1, v_id_caso, v_create + interval '1 day'
            FROM ciudadano c WHERE c.dpi = v_dpi;
        ELSIF v_estado = 'Requiere Ampliación' THEN
            INSERT INTO notificacion(evento, destinatario, cuerpo, resultado, intentos, id_caso, create_at)
            SELECT 'requerimiento de ampliación', c.correo,
                   'Su caso ' || v_codigo || ' requiere ampliación de información.', 'ENVIADA', 1, v_id_caso, v_create + interval '1 day'
            FROM ciudadano c WHERE c.dpi = v_dpi;
        ELSIF v_estado IN ('En Atención', 'En Supervisión', 'Pendiente de Firma') AND v_tecnico IS NOT NULL THEN
            INSERT INTO notificacion(evento, destinatario, cuerpo, resultado, intentos, id_caso, create_at)
            SELECT 'asignación a Técnico', u.correo,
                   'Le fue asignado el caso ' || v_codigo || '.', 'ENVIADA', 1, v_id_caso, v_create + interval '4 hour'
            FROM usuario u WHERE u.cui = v_tecnico;
        END IF;
    END LOOP;

    FOR i IN 1..300 LOOP
        SELECT u.correo, r.rol INTO v_correo, v_rol_texto
        FROM usuario u
        JOIN rol r ON r.id = u.id_rol
        WHERE u.id_estado = v_id_activo AND length(u.correo) <= 50
        ORDER BY u.cui
        OFFSET (i % GREATEST((SELECT count(*) FROM usuario x WHERE x.id_estado = v_id_activo AND length(x.correo) <= 50), 1))
        LIMIT 1;

        v_momento := date_trunc('day', now())
                     - (1 + (i % 360))::int * interval '1 day'
                     + (7 + (i % 10))::int * interval '1 hour'
                     + ((i * 5) % 60) * interval '1 minute';

        INSERT INTO bitacora_cambio(
            accion, usuario, id_caso, fecha, hora, ip_origen,
            detalle, rol, entidad, valor_anterior, valor_nuevo
        ) VALUES (
            'Inicio de sesión', v_correo, NULL, v_momento::date,
            date_trunc('second', v_momento)::time,
            '10.1.' || (i % 200)::text || '.' || (1 + (i % 250))::text,
            'Empleado', v_rol_texto, 'Sesión', NULL, NULL
        );
    END LOOP;

    FOR i IN 1..80 LOOP
        SELECT u.correo, r.rol INTO v_correo, v_rol_texto
        FROM usuario u
        JOIN rol r ON r.id = u.id_rol
        WHERE length(u.correo) <= 50
        ORDER BY u.cui
        OFFSET (i % GREATEST((SELECT count(*) FROM usuario x WHERE length(x.correo) <= 50), 1))
        LIMIT 1;

        v_momento := date_trunc('day', now())
                     - (1 + (i % 180))::int * interval '1 day'
                     + (8 + (i % 8))::int * interval '1 hour';

        INSERT INTO bitacora_cambio(
            accion, usuario, id_caso, fecha, hora, ip_origen,
            detalle, rol, entidad, valor_anterior, valor_nuevo
        ) VALUES (
            'Inicio de sesión fallido', v_correo, NULL, v_momento::date,
            date_trunc('second', v_momento)::time,
            '10.2.' || (i % 200)::text || '.' || (1 + (i % 250))::text,
            'Empleado', v_rol_texto, 'Sesión', NULL, NULL
        );
    END LOOP;

    UPDATE usuario u
    SET cantidad_casos_asignados = (
        SELECT COUNT(*)::INT
        FROM caso c
        JOIN estado_caso e ON e.id = c.id_estado
        WHERE c.id_tecnico = u.cui
          AND e.estado = 'En Atención'
    )
    WHERE u.id_rol = v_id_rol_tec;

    RAISE NOTICE 'Carga de volumen: % casos MG-7', v_total;
END
$vol$;
