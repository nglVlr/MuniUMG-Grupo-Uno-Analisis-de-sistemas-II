package gt.muni.quejas.repository;

import gt.muni.quejas.model.Caso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard 2: las consultas de las cuatro graficas.
 *
 * Son SQL puro (nativeQuery = true), escritas tal como se ejecutan en la base.
 * Se pueden copiar y pegar en DBeaver o en psql sin cambiar nada: los :parametros
 * los pide DBeaver en un cuadro de dialogo. En psql hay que reemplazarlos a mano,
 * por ejemplo :desde por '2025-11-01' y :idDepartamento por NULL.
 *
 * LOS CUATRO FILTROS son siempre los mismos y estan escritos igual en las cinco
 * consultas, en el mismo orden:
 *
 *     1. rango de fechas   :desde y :hasta        (obligatorio)
 *     2. departamento      :idDepartamento        (NULL = todos)
 *     3. tipo de caso      :tipoCaso              (NULL = todos)
 *     4. categoria         :idCategoria           (NULL = todas)
 *
 * PARA AGREGAR UN FILTRO NUEVO, por ejemplo el estado del caso:
 *     1. copiar una linea del WHERE en las cinco consultas y cambiarle el nombre:
 *            AND (CAST(:estado AS VARCHAR) IS NULL OR ec.estado = :estado)
 *     2. agregar el parametro al metodo:    @Param("estado") String estado
 *     3. recibirlo en Dashboard2Service y en Dashboard2Controller
 *
 * El CAST de cada parametro es necesario porque Postgres no adivina el tipo de un
 * parametro cuando llega en NULL. Sin el CAST da el error
 * "could not determine data type of parameter".
 */
public interface Dashboard2Repository extends JpaRepository<Caso, Long> {

    // Cada fila de las graficas de barras y de pastel
    interface FilaValorEtiqueta {
        String getEtiqueta();
        Double getValor();
    }

    // Cada punto de la grafica de dispersion
    interface FilaDispersion {
        String getTecnico();
        Double getCantidad();
        Double getPromedio();
    }

    // El resumen de la campana de Gauss
    interface FilaResumen {
        Double getMedia();
        Double getDesviacion();
    }

    // ---------- 1. BARRAS VERTICALES: cuantos casos hay en cada estado ----------
    @Query(value = """
            SELECT ec.estado AS etiqueta, COUNT(*) AS valor
            FROM caso c
            JOIN estado_caso ec ON ec.id = c.id_estado
            WHERE c.create_at >= CAST(:desde AS DATE)
              AND c.create_at <  CAST(:hasta AS DATE) + 1
              AND (CAST(:idDepartamento AS BIGINT)  IS NULL OR c.id_departamento = :idDepartamento)
              AND (CAST(:tipoCaso       AS VARCHAR) IS NULL OR c.tipo_caso       = :tipoCaso)
              AND (CAST(:idCategoria    AS BIGINT)  IS NULL OR c.id_categoria    = :idCategoria)
            GROUP BY ec.estado
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<FilaValorEtiqueta> casosPorEstado(@Param("desde") LocalDate desde,
                                           @Param("hasta") LocalDate hasta,
                                           @Param("idDepartamento") Long idDepartamento,
                                           @Param("tipoCaso") String tipoCaso,
                                           @Param("idCategoria") Long idCategoria);

    // ---------- 2. PASTEL: cuantos casos hay de cada tipo ----------
    @Query(value = """
            SELECT c.tipo_caso AS etiqueta, COUNT(*) AS valor
            FROM caso c
            WHERE c.create_at >= CAST(:desde AS DATE)
              AND c.create_at <  CAST(:hasta AS DATE) + 1
              AND (CAST(:idDepartamento AS BIGINT)  IS NULL OR c.id_departamento = :idDepartamento)
              AND (CAST(:tipoCaso       AS VARCHAR) IS NULL OR c.tipo_caso       = :tipoCaso)
              AND (CAST(:idCategoria    AS BIGINT)  IS NULL OR c.id_categoria    = :idCategoria)
            GROUP BY c.tipo_caso
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<FilaValorEtiqueta> casosPorTipo(@Param("desde") LocalDate desde,
                                         @Param("hasta") LocalDate hasta,
                                         @Param("idDepartamento") Long idDepartamento,
                                         @Param("tipoCaso") String tipoCaso,
                                         @Param("idCategoria") Long idCategoria);

    // ---------- 3. DISPERSION: cada Tecnico, sus casos contra sus dias promedio ----------
    // El LEFT JOIN trae la firma del oficio, que es cuando el caso quedo resuelto.
    // AVG no toma en cuenta los casos sin firma, asi que el promedio sale solo de
    // los que ya se resolvieron. Se agrupa tambien por cui porque hay Tecnicos que
    // se llaman igual y si no quedarian sumados en un solo punto.
    // El HAVING deja fuera al Tecnico que todavia no ha resuelto nada: su promedio
    // seria cero y en la grafica se veria como si resolviera en cero dias.
    @Query(value = """
            SELECT u.nombre AS tecnico,
                   COUNT(*) AS cantidad,
                   COALESCE(ROUND(AVG(EXTRACT(EPOCH FROM ((b.fecha + b.hora) - c.create_at))
                                      / 86400.0)::numeric, 2), 0) AS promedio
            FROM caso c
            JOIN usuario u ON u.cui = c.id_tecnico
            LEFT JOIN bitacora_cambio b ON b.id_caso = c.id
                                       AND b.accion = 'Firma de oficio de resolución'
            WHERE c.create_at >= CAST(:desde AS DATE)
              AND c.create_at <  CAST(:hasta AS DATE) + 1
              AND (CAST(:idDepartamento AS BIGINT)  IS NULL OR c.id_departamento = :idDepartamento)
              AND (CAST(:tipoCaso       AS VARCHAR) IS NULL OR c.tipo_caso       = :tipoCaso)
              AND (CAST(:idCategoria    AS BIGINT)  IS NULL OR c.id_categoria    = :idCategoria)
            GROUP BY u.cui, u.nombre
            HAVING COUNT(b.id) > 0
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<FilaDispersion> casosPorTecnico(@Param("desde") LocalDate desde,
                                         @Param("hasta") LocalDate hasta,
                                         @Param("idDepartamento") Long idDepartamento,
                                         @Param("tipoCaso") String tipoCaso,
                                         @Param("idCategoria") Long idCategoria);

    // ---------- 4a. CAMPANA DE GAUSS: las barras, en tramos de dos dias ----------
    // dias es cuanto tardo el caso: la firma del oficio menos la fecha de registro.
    // FLOOR(dias / 2) * 2 + 1 es el centro del tramo: 1, 3, 5, 7...
    @Query(value = """
            SELECT CAST(FLOOR(t.dias / 2) * 2 + 1 AS VARCHAR) AS etiqueta,
                   COUNT(*) AS valor
            FROM (
                SELECT EXTRACT(EPOCH FROM ((b.fecha + b.hora) - c.create_at)) / 86400.0 AS dias
                FROM caso c
                JOIN bitacora_cambio b ON b.id_caso = c.id
                                      AND b.accion = 'Firma de oficio de resolución'
                WHERE c.create_at >= CAST(:desde AS DATE)
                  AND c.create_at <  CAST(:hasta AS DATE) + 1
                  AND (CAST(:idDepartamento AS BIGINT)  IS NULL OR c.id_departamento = :idDepartamento)
                  AND (CAST(:tipoCaso       AS VARCHAR) IS NULL OR c.tipo_caso       = :tipoCaso)
                  AND (CAST(:idCategoria    AS BIGINT)  IS NULL OR c.id_categoria    = :idCategoria)
            ) t
            GROUP BY FLOOR(t.dias / 2)
            ORDER BY FLOOR(t.dias / 2)
            """, nativeQuery = true)
    List<FilaValorEtiqueta> diasDeResolucionPorTramo(@Param("desde") LocalDate desde,
                                                     @Param("hasta") LocalDate hasta,
                                                     @Param("idDepartamento") Long idDepartamento,
                                                     @Param("tipoCaso") String tipoCaso,
                                                     @Param("idCategoria") Long idCategoria);

    // ---------- 4b. CAMPANA DE GAUSS: la media y la desviacion, para la curva ----------
    @Query(value = """
            SELECT COALESCE(ROUND(AVG(t.dias)::numeric, 2), 0)         AS media,
                   COALESCE(ROUND(STDDEV_SAMP(t.dias)::numeric, 2), 0) AS desviacion
            FROM (
                SELECT EXTRACT(EPOCH FROM ((b.fecha + b.hora) - c.create_at)) / 86400.0 AS dias
                FROM caso c
                JOIN bitacora_cambio b ON b.id_caso = c.id
                                      AND b.accion = 'Firma de oficio de resolución'
                WHERE c.create_at >= CAST(:desde AS DATE)
                  AND c.create_at <  CAST(:hasta AS DATE) + 1
                  AND (CAST(:idDepartamento AS BIGINT)  IS NULL OR c.id_departamento = :idDepartamento)
                  AND (CAST(:tipoCaso       AS VARCHAR) IS NULL OR c.tipo_caso       = :tipoCaso)
                  AND (CAST(:idCategoria    AS BIGINT)  IS NULL OR c.id_categoria    = :idCategoria)
            ) t
            """, nativeQuery = true)
    FilaResumen mediaYDesviacion(@Param("desde") LocalDate desde,
                                 @Param("hasta") LocalDate hasta,
                                 @Param("idDepartamento") Long idDepartamento,
                                 @Param("tipoCaso") String tipoCaso,
                                 @Param("idCategoria") Long idCategoria);
}
