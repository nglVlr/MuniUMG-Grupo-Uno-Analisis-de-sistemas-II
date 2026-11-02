-- =====================================================================
-- CU-19 Dashboard de graficas: consultas que respaldan cada grafica
-- =====================================================================
--
-- El backend no ejecuta una consulta por grafica. Ejecuta dos consultas
-- (CasoRepository.buscarParaDashboard y BitacoraCambioRepository) y agrupa
-- el resultado en Java. Este archivo traduce esa logica a SQL puro, para
-- poder comprobar en la base los numeros que muestra la pantalla.
--
-- Como usarlo:
--   docker exec -i quejas-db psql -U quejas_user -d quejas_db < consultas_dashboard.sql
-- o bien, pegando los bloques uno por uno en psql.
--
-- Los filtros se escriben UNA sola vez, en la vista "filtros" de abajo.
-- Dejar un filtro en NULL significa "todos", igual que en la pantalla.
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Los 10 filtros de la pantalla. Es lo unico que hay que editar.
-- ---------------------------------------------------------------------
DROP VIEW IF EXISTS casos_filtrados CASCADE;
DROP VIEW IF EXISTS resolucion CASCADE;
DROP VIEW IF EXISTS filtros CASCADE;

CREATE TEMP VIEW filtros AS
SELECT
    DATE '2025-11-02'   AS desde,              -- Desde
    DATE '2026-11-02'   AS hasta,              -- Hasta
    'registro'::text    AS modo_fecha,         -- 'registro' o 'resolucion'
    NULL::bigint        AS id_departamento,    -- Departamento
    NULL::varchar       AS tipo_caso,          -- Queja, Reclamo, Denuncia, Sugerencia
    NULL::bigint        AS id_categoria,       -- Categoria
    NULL::varchar       AS estado,             -- Estado actual
    NULL::varchar       AS cui_tecnico,        -- Tecnico
    NULL::bigint        AS id_motivo_rechazo,  -- Motivo de rechazo
    NULL::varchar       AS cargo,              -- Ciudadano, Auxiliar, Tecnico, Supervisor, Jefe, Admin
    NULL::varchar       AS dpi_ciudadano;      -- Ciudadano por DPI (13 digitos)


-- ---------------------------------------------------------------------
-- 2. Fecha de resolucion. No esta en la tabla caso: se deduce de la
--    bitacora, de la accion con la que la Jefatura firma el oficio.
--    La fecha y la hora estan en columnas separadas y se combinan.
-- ---------------------------------------------------------------------
CREATE TEMP VIEW resolucion AS
SELECT b.id_caso,
       max(b.fecha + b.hora) AS momento
FROM bitacora_cambio b
WHERE b.accion = 'Firma de oficio de resolución'
GROUP BY b.id_caso;


-- ---------------------------------------------------------------------
-- 3. Los filtros aplicados una sola vez sobre los casos.
--    Todas las graficas de abajo leen de aqui, igual que el backend.
-- ---------------------------------------------------------------------
CREATE TEMP VIEW casos_filtrados AS
SELECT c.id,
       c.codigo_seguimiento,
       c.create_at,
       r.momento                       AS fecha_resolucion,
       c.tipo_caso,
       ec.estado,
       d.nombre                        AS departamento,
       cat.nombre                      AS categoria,
       ci.nombre                       AS ciudadano,
       ci.dpi,
       COALESCE(u.nombre, 'Sin asignar')  AS tecnico,
       COALESCE(mr.motivo, 'Sin rechazo') AS motivo_rechazo,
       -- El cargo son los casos que hoy estan en manos de ese cargo
       CASE
           WHEN ec.estado IN ('Registrado', 'En Revisión') THEN 'Auxiliar'
           WHEN ec.estado = 'En Atención' THEN 'Tecnico'
           WHEN ec.estado IN ('En Supervisión', 'Pendiente de Aprobación de Rechazo') THEN 'Supervisor'
           WHEN ec.estado = 'Pendiente de Firma' THEN 'Jefe'
           WHEN ec.estado = 'Requiere Ampliación' THEN 'Ciudadano'
           ELSE 'Caso cerrado'
       END                             AS cargo,
       -- La fecha con la que se filtra y se agrupa por tiempo
       CASE WHEN f.modo_fecha = 'resolucion' THEN r.momento ELSE c.create_at END AS fecha_base
FROM caso c
JOIN estado_caso ec     ON ec.id = c.id_estado
JOIN departamento d     ON d.id = c.id_departamento
JOIN categoria cat      ON cat.id = c.id_categoria
JOIN ciudadano ci       ON ci.dpi = c.dpi
LEFT JOIN usuario u     ON u.cui = c.id_tecnico
LEFT JOIN motivo_rechazo mr ON mr.id = c.id_motivo_rechazo
LEFT JOIN resolucion r  ON r.id_caso = c.id
CROSS JOIN filtros f
WHERE (f.id_departamento   IS NULL OR c.id_departamento = f.id_departamento)
  AND (f.tipo_caso         IS NULL OR c.tipo_caso = f.tipo_caso)
  AND (f.id_categoria      IS NULL OR c.id_categoria = f.id_categoria)
  AND (f.estado            IS NULL OR ec.estado = f.estado)
  AND (f.cui_tecnico       IS NULL OR c.id_tecnico = f.cui_tecnico)
  AND (f.id_motivo_rechazo IS NULL OR c.id_motivo_rechazo = f.id_motivo_rechazo)
  AND (f.dpi_ciudadano     IS NULL OR c.dpi = f.dpi_ciudadano)
  -- Por fecha de resolucion solo entran los casos ya resueltos
  AND (CASE WHEN f.modo_fecha = 'resolucion' THEN r.momento ELSE c.create_at END) >= f.desde
  AND (CASE WHEN f.modo_fecha = 'resolucion' THEN r.momento ELSE c.create_at END) <  f.hasta + 1
  AND (f.cargo IS NULL OR f.cargo = CASE
           WHEN ec.estado IN ('Registrado', 'En Revisión') THEN 'Auxiliar'
           WHEN ec.estado = 'En Atención' THEN 'Tecnico'
           WHEN ec.estado IN ('En Supervisión', 'Pendiente de Aprobación de Rechazo') THEN 'Supervisor'
           WHEN ec.estado = 'Pendiente de Firma' THEN 'Jefe'
           WHEN ec.estado = 'Requiere Ampliación' THEN 'Ciudadano'
           ELSE 'Caso cerrado' END);


\echo '=== Total de casos que pasaron los filtros ==='
SELECT count(*) AS total_casos FROM casos_filtrados;


-- =====================================================================
-- METRICAS. Cada una cambia solo la columna que se calcula.
-- =====================================================================

\echo '=== Metrica: conteo de casos, agrupado por estado ==='
SELECT estado, count(*) AS valor
FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Metrica: casos resueltos, agrupado por departamento ==='
SELECT departamento, count(*) FILTER (WHERE estado = 'Resuelto') AS valor
FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Metrica: porcentaje de resolucion, agrupado por departamento ==='
SELECT departamento,
       round(100.0 * count(*) FILTER (WHERE estado = 'Resuelto') / count(*), 2) AS valor
FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Metrica: tiempo promedio de resolucion en dias, por tecnico ==='
-- Tiempo de resolucion = firma del oficio menos registro del caso
SELECT tecnico,
       round(avg(EXTRACT(EPOCH FROM (fecha_resolucion - create_at)) / 86400.0)::numeric, 2) AS dias,
       count(*) AS casos_resueltos
FROM casos_filtrados
WHERE fecha_resolucion IS NOT NULL
GROUP BY 1 ORDER BY 2 DESC;


-- =====================================================================
-- AGRUPACIONES. La metrica se mantiene y cambia el GROUP BY.
-- =====================================================================

\echo '=== Agrupado por tipo de caso ==='
SELECT tipo_caso, count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Agrupado por categoria ==='
SELECT categoria, count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Agrupado por tecnico ==='
SELECT tecnico, count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Agrupado por cargo que lo tiene hoy ==='
SELECT cargo, count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Agrupado por ciudadano ==='
SELECT ciudadano, count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Agrupado por motivo de rechazo ==='
SELECT motivo_rechazo, count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Agrupado por dia / semana / mes ==='
SELECT fecha_base::date AS dia, count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 1;
SELECT to_char(fecha_base, 'IYYY-"S"IW') AS semana, count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 1;
SELECT to_char(fecha_base, 'YYYY-MM')    AS mes,    count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 1;

\echo '=== Agrupado por dia de la semana y por hora ==='
SELECT to_char(fecha_base, 'TMDay') AS dia_semana, count(*) FROM casos_filtrados GROUP BY 1, EXTRACT(ISODOW FROM fecha_base) ORDER BY 2 DESC;
SELECT to_char(fecha_base, 'HH24') || ':00' AS hora, count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 1;


-- =====================================================================
-- DESGLOSE: lo que alimenta las barras apiladas y las lineas multiples
-- =====================================================================

\echo '=== Departamento por estado (barras apiladas) ==='
SELECT departamento, estado, count(*)
FROM casos_filtrados GROUP BY 1, 2 ORDER BY 1, 2;


-- =====================================================================
-- GRAFICAS CON FORMA PROPIA
-- =====================================================================

\echo '=== Treemap: departamento y categoria ==='
SELECT departamento, categoria, count(*) FROM casos_filtrados GROUP BY 1, 2 ORDER BY 1, 3 DESC;

\echo '=== Sunburst: departamento, tipo y estado ==='
SELECT departamento, tipo_caso, estado, count(*) FROM casos_filtrados GROUP BY 1, 2, 3 ORDER BY 1, 2, 4 DESC;

\echo '=== Pareto de motivos de rechazo, con su porcentaje acumulado ==='
SELECT motivo_rechazo,
       count(*) AS casos,
       round(100.0 * sum(count(*)) OVER (ORDER BY count(*) DESC) / sum(count(*)) OVER (), 2) AS acumulado
FROM casos_filtrados
WHERE motivo_rechazo <> 'Sin rechazo'
GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Cascada: de recibidos a resueltos ==='
SELECT count(*)                                                                  AS recibidos,
       -count(*) FILTER (WHERE estado IN ('Rechazado', 'Requiere Ampliación'))   AS rechazados,
       -count(*) FILTER (WHERE estado = 'Pendiente de Aprobación de Rechazo')    AS rechazo_en_tramite,
       -count(*) FILTER (WHERE estado NOT IN ('Resuelto', 'Rechazado', 'Requiere Ampliación',
                                              'Pendiente de Aprobación de Rechazo')) AS en_proceso,
       count(*) FILTER (WHERE estado = 'Resuelto')                               AS resueltos
FROM casos_filtrados;

\echo '=== Histograma de dias de resolucion ==='
SELECT CASE
           WHEN dias <= 2  THEN '0 a 2 días'
           WHEN dias <= 5  THEN '3 a 5 días'
           WHEN dias <= 10 THEN '6 a 10 días'
           WHEN dias <= 20 THEN '11 a 20 días'
           WHEN dias <= 30 THEN '21 a 30 días'
           ELSE 'Más de 30 días'
       END AS tramo,
       count(*)
FROM (SELECT EXTRACT(EPOCH FROM (fecha_resolucion - create_at)) / 86400.0 AS dias
      FROM casos_filtrados WHERE fecha_resolucion IS NOT NULL) t
GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Campana de Gauss: tramos de 2 dias, con media y desviacion ==='
SELECT round(avg(dias)::numeric, 2)        AS media_dias,
       round(stddev_samp(dias)::numeric, 2) AS desviacion_dias,
       count(*)                             AS casos
FROM (SELECT EXTRACT(EPOCH FROM (fecha_resolucion - create_at)) / 86400.0 AS dias
      FROM casos_filtrados WHERE fecha_resolucion IS NOT NULL) t;

SELECT (floor(dias / 2) * 2 + 1)::numeric AS centro_del_tramo, count(*)
FROM (SELECT EXTRACT(EPOCH FROM (fecha_resolucion - create_at)) / 86400.0 AS dias
      FROM casos_filtrados WHERE fecha_resolucion IS NOT NULL) t
GROUP BY 1 ORDER BY 1;

\echo '=== Boxplot: cuartiles de los dias de resolucion por tecnico ==='
SELECT tecnico,
       round(min(dias)::numeric, 2)                                              AS minimo,
       round(percentile_cont(0.25) WITHIN GROUP (ORDER BY dias)::numeric, 2)     AS cuartil_uno,
       round(percentile_cont(0.50) WITHIN GROUP (ORDER BY dias)::numeric, 2)     AS mediana,
       round(percentile_cont(0.75) WITHIN GROUP (ORDER BY dias)::numeric, 2)     AS cuartil_tres,
       round(max(dias)::numeric, 2)                                              AS maximo
FROM (SELECT tecnico, EXTRACT(EPOCH FROM (fecha_resolucion - create_at)) / 86400.0 AS dias
      FROM casos_filtrados WHERE fecha_resolucion IS NOT NULL AND tecnico <> 'Sin asignar') t
GROUP BY 1 ORDER BY 1;

\echo '=== Dispersion y burbujas: carga del tecnico contra su tiempo promedio ==='
SELECT tecnico,
       count(*)                                                  AS casos_a_cargo,
       round(avg(EXTRACT(EPOCH FROM (fecha_resolucion - create_at)) / 86400.0)
             FILTER (WHERE fecha_resolucion IS NOT NULL)::numeric, 2) AS dias_promedio,
       count(*) FILTER (WHERE fecha_resolucion IS NOT NULL)      AS resueltos
FROM casos_filtrados WHERE tecnico <> 'Sin asignar'
GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Mapa de calor: dia de la semana por hora ==='
SELECT to_char(fecha_base, 'TMDay') AS dia_semana,
       to_char(fecha_base, 'HH24') || ':00' AS hora,
       count(*)
FROM casos_filtrados GROUP BY 1, 2, EXTRACT(ISODOW FROM fecha_base) ORDER BY 3 DESC LIMIT 20;

\echo '=== Mapa de calor: categoria por mes ==='
SELECT categoria, to_char(fecha_base, 'YYYY-MM') AS mes, count(*)
FROM casos_filtrados GROUP BY 1, 2 ORDER BY 3 DESC LIMIT 20;

\echo '=== Calendario: casos por dia ==='
SELECT fecha_base::date AS fecha, count(*) FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC LIMIT 15;

\echo '=== Escalonada: casos abiertos acumulados al cierre de cada mes ==='
-- Un caso se cierra al resolverse o al aprobarse su rechazo
WITH cierre AS (
    SELECT c.id,
           c.create_at,
           COALESCE(r.momento,
                    (SELECT max(b.fecha + b.hora) FROM bitacora_cambio b
                     WHERE b.id_caso = c.id AND b.accion = 'Aprobación de rechazo'
                       AND b.valor_nuevo = 'Rechazado')) AS momento_cierre
    FROM casos_filtrados c LEFT JOIN resolucion r ON r.id_caso = c.id
), meses AS (
    SELECT DISTINCT to_char(create_at, 'YYYY-MM') AS mes FROM cierre
)
SELECT m.mes,
       count(*) FILTER (WHERE to_char(c.create_at, 'YYYY-MM') <= m.mes
                          AND (c.momento_cierre IS NULL
                               OR to_char(c.momento_cierre, 'YYYY-MM') > m.mes)) AS abiertos
FROM meses m CROSS JOIN cierre c
GROUP BY 1 ORDER BY 1;

\echo '=== Ranking de ciudadanos: ingresados, rechazados y rechazo en tramite ==='
-- Rechazados son Rechazado y Requiere Ampliacion.
-- Pendiente de Aprobacion de Rechazo se muestra aparte, como rechazo en tramite.
SELECT ciudadano,
       count(*)                                                                AS ingresados,
       count(*) FILTER (WHERE estado IN ('Rechazado', 'Requiere Ampliación'))  AS rechazados,
       count(*) FILTER (WHERE estado = 'Pendiente de Aprobación de Rechazo')   AS rechazo_en_tramite
FROM casos_filtrados GROUP BY 1 ORDER BY 2 DESC LIMIT 10;

\echo '=== Tabla de Tecnicos con semaforo ==='
-- Verde hasta 10 dias, amarillo hasta 20, rojo arriba de eso.
-- Sin casos resueltos todavia no hay tiempo que calificar: queda amarillo.
SELECT tecnico,
       max(departamento)                                        AS departamento,
       count(*) FILTER (WHERE estado = 'En Atención')            AS en_atencion,
       count(*) FILTER (WHERE fecha_resolucion IS NOT NULL)      AS resueltos,
       COALESCE(round(avg(EXTRACT(EPOCH FROM (fecha_resolucion - create_at)) / 86400.0)
                      FILTER (WHERE fecha_resolucion IS NOT NULL)::numeric, 2), 0) AS dias_promedio,
       CASE
           WHEN count(*) FILTER (WHERE fecha_resolucion IS NOT NULL) = 0 THEN 'amarillo'
           WHEN avg(EXTRACT(EPOCH FROM (fecha_resolucion - create_at)) / 86400.0)
                FILTER (WHERE fecha_resolucion IS NOT NULL) <= 10 THEN 'verde'
           WHEN avg(EXTRACT(EPOCH FROM (fecha_resolucion - create_at)) / 86400.0)
                FILTER (WHERE fecha_resolucion IS NOT NULL) <= 20 THEN 'amarillo'
           ELSE 'rojo'
       END AS semaforo
FROM casos_filtrados WHERE tecnico <> 'Sin asignar'
GROUP BY 1 ORDER BY 1;


-- =====================================================================
-- GRAFICAS QUE SALEN DE LA BITACORA, NO DE LA TABLA CASO
-- =====================================================================

\echo '=== Embudo: cuantos casos llegaron a cada etapa ==='
-- El estado al que pasa el caso se deduce de la accion que quedo en la bitacora
SELECT etapa, count(DISTINCT id_caso) AS casos
FROM (
    SELECT b.id_caso,
           CASE b.accion
               WHEN 'Registro de caso'                  THEN 'Registrado'
               WHEN 'Aceptación de caso'                THEN 'En Revisión'
               WHEN 'Asignación de Técnico'             THEN 'En Atención'
               WHEN 'Finalización de atención'          THEN 'En Supervisión'
               WHEN 'Aprobación de reporte de atención' THEN 'Pendiente de Firma'
               WHEN 'Firma de oficio de resolución'     THEN 'Resuelto'
               WHEN 'Devolución al Técnico'             THEN 'En Atención'
               WHEN 'Devolución a Supervisión'          THEN 'En Supervisión'
               WHEN 'Ampliación de caso'                THEN 'En Revisión'
               WHEN 'Rechazo no aprobado'               THEN 'En Revisión'
           END AS etapa
    FROM bitacora_cambio b
    WHERE b.id_caso IN (SELECT id FROM casos_filtrados)
) t
WHERE etapa IN ('Registrado','En Revisión','En Atención','En Supervisión','Pendiente de Firma','Resuelto')
GROUP BY 1
ORDER BY CASE etapa
             WHEN 'Registrado' THEN 1 WHEN 'En Revisión' THEN 2 WHEN 'En Atención' THEN 3
             WHEN 'En Supervisión' THEN 4 WHEN 'Pendiente de Firma' THEN 5 ELSE 6 END;

\echo '=== Sankey: transiciones de un estado al siguiente ==='
-- La pantalla ademas nombra con su vuelta los estados a los que el caso regresa
-- por una devolucion o una ampliacion, para que el flujo no se cicle.
WITH pasos AS (
    SELECT b.id_caso, b.id,
           CASE b.accion
               WHEN 'Registro de caso'                  THEN 'Registrado'
               WHEN 'Aceptación de caso'                THEN 'En Revisión'
               WHEN 'Asignación de Técnico'             THEN 'En Atención'
               WHEN 'Finalización de atención'          THEN 'En Supervisión'
               WHEN 'Aprobación de reporte de atención' THEN 'Pendiente de Firma'
               WHEN 'Firma de oficio de resolución'     THEN 'Resuelto'
               WHEN 'Devolución al Técnico'             THEN 'En Atención'
               WHEN 'Devolución a Supervisión'          THEN 'En Supervisión'
               WHEN 'Rechazo de caso'                   THEN 'Pendiente de Aprobación de Rechazo'
               WHEN 'Rechazo con ampliación'            THEN 'Requiere Ampliación'
               WHEN 'Aprobación de rechazo'             THEN COALESCE(b.valor_nuevo, 'Rechazado')
               WHEN 'Ampliación de caso'                THEN 'En Revisión'
               WHEN 'Rechazo no aprobado'               THEN 'En Revisión'
           END AS estado
    FROM bitacora_cambio b
    WHERE b.id_caso IN (SELECT id FROM casos_filtrados)
)
SELECT previo AS origen, estado AS destino, count(*) AS casos
FROM (SELECT id_caso, estado, lag(estado) OVER (PARTITION BY id_caso ORDER BY id) AS previo
      FROM pasos WHERE estado IS NOT NULL) t
WHERE previo IS NOT NULL AND previo <> estado
GROUP BY 1, 2 ORDER BY 3 DESC;

\echo '=== Trazabilidad de un caso: su linea de tiempo ==='
SELECT b.accion, b.usuario, b.rol, b.fecha, b.hora, b.ip_origen, b.detalle
FROM bitacora_cambio b
JOIN caso c ON c.id = b.id_caso
WHERE c.codigo_seguimiento = 'MG-8108-2026'   -- cambiar por el codigo que se quiera ver
ORDER BY b.id;

\echo '=== Actividad del sistema: acciones, inicios de sesion y errores ==='
-- Estas graficas no pertenecen a un caso: solo respetan fecha, departamento del
-- usuario, cargo y usuario. Por eso en la pantalla los demas filtros se deshabilitan.
SELECT b.accion, count(*) AS veces
FROM bitacora_cambio b CROSS JOIN filtros f
LEFT JOIN usuario u ON u.cui = b.usuario OR u.correo = b.usuario
WHERE b.fecha BETWEEN f.desde AND f.hasta
  AND (f.cargo           IS NULL OR b.rol = f.cargo)
  AND (f.id_departamento IS NULL OR u.id_departamento = f.id_departamento)
GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Acciones por usuario, con lo que hizo con el caso ==='
-- Aprobo, rechazo o devolvio: asi se clasifican las acciones de cada cargo
SELECT COALESCE(u.nombre, b.usuario) AS usuario,
       b.rol AS cargo,
       count(*) FILTER (WHERE b.accion IN ('Aceptación de caso', 'Aprobación de reporte de atención',
                                           'Aprobación de rechazo', 'Firma de oficio de resolución')) AS aprobo,
       count(*) FILTER (WHERE b.accion IN ('Rechazo de caso', 'Rechazo con ampliación',
                                           'Rechazo no aprobado'))                                    AS rechazo,
       count(*) FILTER (WHERE b.accion IN ('Devolución al Técnico', 'Devolución a Supervisión'))       AS devolvio,
       count(*)                                                                                        AS total
FROM bitacora_cambio b
LEFT JOIN usuario u ON u.cui = b.usuario OR u.correo = b.usuario
CROSS JOIN filtros f
WHERE b.fecha BETWEEN f.desde AND f.hasta
GROUP BY 1, 2 ORDER BY 6 DESC LIMIT 15;

\echo '=== Tarjetas de indicadores ==='
SELECT (SELECT count(*) FROM casos_filtrados)                                        AS casos_recibidos,
       (SELECT count(*) FROM casos_filtrados WHERE estado = 'Resuelto')              AS casos_resueltos,
       (SELECT round(100.0 * count(*) FILTER (WHERE estado = 'Resuelto') / count(*), 2)
          FROM casos_filtrados)                                                      AS porcentaje_resolucion,
       (SELECT round(avg(EXTRACT(EPOCH FROM (fecha_resolucion - create_at)) / 86400.0)::numeric, 2)
          FROM casos_filtrados WHERE fecha_resolucion IS NOT NULL)                   AS dias_promedio,
       (SELECT count(*) FROM bitacora_cambio b CROSS JOIN filtros f
         WHERE b.fecha BETWEEN f.desde AND f.hasta)                                  AS acciones_bitacora,
       (SELECT count(*) FROM bitacora_cambio b CROSS JOIN filtros f
         WHERE b.fecha BETWEEN f.desde AND f.hasta AND b.accion = 'Inicio de sesión') AS login_exitoso,
       (SELECT count(*) FROM bitacora_cambio b CROSS JOIN filtros f
         WHERE b.fecha BETWEEN f.desde AND f.hasta AND b.accion = 'Inicio de sesión fallido') AS login_fallido,
       (SELECT count(*) FROM bitacora_cambio b CROSS JOIN filtros f
         WHERE b.fecha BETWEEN f.desde AND f.hasta AND b.accion = 'Error envío de notificación') AS errores_notificacion;
